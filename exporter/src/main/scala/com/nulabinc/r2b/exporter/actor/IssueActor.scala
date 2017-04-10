package com.nulabinc.r2b.exporter.actor

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.conf.BacklogPaths
import com.nulabinc.backlog.migration.converter.Convert
import com.nulabinc.backlog.migration.domain.{BacklogComment, BacklogIssue}
import com.nulabinc.backlog.migration.utils.{DateUtil, IOUtil, Logging}
import com.nulabinc.r2b.exporter.convert.{IssueWrites, JournalWrites, UserWrites}
import com.nulabinc.r2b.exporter.service.{CommentReducer, IssueInitializer}
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.service.{IssueService, ProjectService}
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Attachment, _}
import spray.json._
import com.nulabinc.backlog.migration.domain.BacklogJsonProtocol._

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueActor(apiConfig: RedmineConfig,
                 backlogPaths: BacklogPaths,
                 issueService: IssueService,
                 projectService: ProjectService,
                 issueWrites: IssueWrites,
                 journalWrites: JournalWrites,
                 userWrites: UserWrites)
    extends Actor
    with Logging {

  def receive: Receive = {
    case IssueActor.Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit)) =>
      val issue    = issueService.issueOfId(issueId, Include.attachments, Include.journals)
      val journals = issue.getJournals.asScala.toSeq

      val attachments: Seq[Attachment] = issue.getAttachments.asScala.toSeq

      val backlogIssue = exportIssue(issue, journals)
      exportComments(backlogIssue, journals.map(Convert.toBacklog(_)(journalWrites)), attachments)

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

  private[this] def exportIssue(issue: Issue, journals: Seq[Journal]): BacklogIssue = {
    val issueCreated = DateUtil.tryIsoParse(Option(issue.getCreatedOn).map(DateUtil.isoFormat))
    val issueDirPath =
      backlogPaths.issueDirectoryPath(DateUtil.yyyymmddFormat(issueCreated), s"${issueCreated.getTime}-${issue.getId.intValue()}-issue-0")
    val issueInitializer = new IssueInitializer(issueWrites, userWrites, issueService, journals)
    val backlogIssue     = issueInitializer.initialize(issue)
    IOUtil.output(
      backlogPaths.issueJson(issueDirPath),
      backlogIssue.toJson.prettyPrint
    )
    backlogIssue
  }

  private[this] def exportComments(issue: BacklogIssue, comments: Seq[BacklogComment], attachments: Seq[Attachment]) = {
    comments.zipWithIndex.foreach {
      case (comment, index) =>
        exportComment(comment, issue, comments, attachments, index)
    }
  }

  private[this] def exportComment(comment: BacklogComment,
                                  issue: BacklogIssue,
                                  comments: Seq[BacklogComment],
                                  attachments: Seq[Attachment],
                                  index: Int) = {
    val commentCreated = DateUtil.tryIsoParse(comment.optCreated)
    val issueDirPath =
      backlogPaths.issueDirectoryPath(DateUtil.yyyymmddFormat(commentCreated), s"${commentCreated.getTime}-${issue.id}-comment-${index}")

    val commentReducer =
      new CommentReducer(apiConfig: RedmineConfig, issueService, projectService, backlogPaths, issue, comments, attachments, issueDirPath)
    val reduced = commentReducer.reduce(comment)

    IOUtil.output(backlogPaths.issueJson(issueDirPath), reduced.toJson.prettyPrint)
  }

}

object IssueActor {

  case class Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit))

}

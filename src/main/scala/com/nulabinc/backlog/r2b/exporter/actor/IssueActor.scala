package com.nulabinc.backlog.r2b.exporter.actor

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.common.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain.{BacklogComment, BacklogIssue, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, IOUtil, Logging}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.nulabinc.backlog.r2b.exporter.service.{ChangeLogReducer, CommentReducer, IssueInitializer}
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Attachment, _}
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author uchida
  */
private[exporter] class IssueActor(exportContext: ExportContext, backlogTextFormattingRule: BacklogTextFormattingRule) extends Actor with Logging {

  implicit val issueWrites   = exportContext.issueWrites
  implicit val journalWrites = exportContext.journalWrites

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    logger.debug(s"preRestart: reason: ${reason}, message: ${message}")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(10.seconds, self, value)
    }
  }

  def receive: Receive = {
    case IssueActor.Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit)) =>
      logger.debug(s"[START ISSUE]${issueId} thread numbers:${java.lang.Thread.activeCount()}")

      val issue                        = exportContext.issueService.issueOfId(issueId, Include.attachments, Include.journals)
      val journals                     = issue.getJournals.asScala.toSeq.sortWith((c1, c2) => c1.getCreatedOn.before(c2.getCreatedOn))
      val attachments: Seq[Attachment] = issue.getAttachments.asScala.toSeq

      exportIssue(issue, journals, attachments)
      exportComments(issue, journals, attachments)

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

  private[this] def exportIssue(issue: Issue, journals: Seq[Journal], attachments: Seq[Attachment]) = {
    val issueCreated     = DateUtil.tryIsoParse(Option(issue.getCreatedOn).map(DateUtil.isoFormat))
    val issueDirPath     = exportContext.backlogPaths.issueDirectoryPath("issue", issue.getId.intValue(), issueCreated, 0)
    val issueInitializer = new IssueInitializer(exportContext, issueDirPath, journals, attachments, backlogTextFormattingRule)
    val backlogIssue     = issueInitializer.initialize(issue)

    IOUtil.output(exportContext.backlogPaths.issueJson(issueDirPath), backlogIssue.toJson.prettyPrint)
  }

  private[this] def exportComments(issue: Issue, journals: Seq[Journal], attachments: Seq[Attachment]) = {
    val backlogIssue    = Convert.toBacklog(issue)
    val backlogComments = journals.map(Convert.toBacklog(_))
    backlogComments.zipWithIndex.foreach {
      case (comment, index) =>
        exportComment(comment, backlogIssue, backlogComments, attachments, index)
    }
  }

  private[this] def exportComment(comment: BacklogComment,
                                  issue: BacklogIssue,
                                  comments: Seq[BacklogComment],
                                  attachments: Seq[Attachment],
                                  index: Int) = {
    val commentCreated   = DateUtil.tryIsoParse(comment.optCreated)
    val issueDirPath     = exportContext.backlogPaths.issueDirectoryPath("comment", issue.id, commentCreated, index)
    val changeLogReducer = new ChangeLogReducer(exportContext, issueDirPath, issue, comments, attachments)
    val commentReducer   = new CommentReducer(issue.id, changeLogReducer)
    val reduced          = commentReducer.reduce(comment)

    IOUtil.output(exportContext.backlogPaths.issueJson(issueDirPath), reduced.toJson.prettyPrint)
  }

}

private[exporter] object IssueActor {

  case class Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit))

}

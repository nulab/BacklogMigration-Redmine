package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Attachment, Issue, Project, User}

/**
 * @author uchida
 */
class IssuesActor(r2bConf: R2BConfig, project: Project) extends Actor with R2BLogging {

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  private val redmineService: RedmineService = new RedmineService(r2bConf)

  private var allCount: Int = 0
  private var count: Int = 0

  def receive: Receive = {
    case IssuesActor.Do =>
      allCount = redmineService.getIssuesCount(project.getId)

      if (allCount != 0) printlog(Messages("message.execute_redmine_issues_export", project.getName, allCount))

      issuesLoop(0)
      context.stop(self)
  }

  private def issuesLoop(n: Int): Unit =
    if (n < allCount) {
      searchIssues(n)
      issuesLoop(n + Redmine.ISSUE_GET_LIMIT)
    }

  private def searchIssues(offset: Int) = {
    val params: Map[String, String] = Map("offset" -> offset.toString, "limit" -> Redmine.ISSUE_GET_LIMIT.toString, "project_id" -> project.getId.toString, "status_id" -> "*")
    val issues: Seq[Issue] = redmineService.getIssues(params)
    issues.foreach(output)
  }

  private def output(searchIssue: Issue) = {
    val issue: Issue = redmineService.getIssueById(searchIssue.getId, Include.attachments, Include.journals)
    val users: Seq[User] = redmineService.getUsers

    IOUtil.output(ConfigBase.Redmine.getIssuePath(project.getIdentifier, searchIssue.getId), RedmineMarshaller.Issue(issue, project, users))

    val attachments: Array[Attachment] = issue.getAttachments.toArray(new Array[Attachment](issue.getAttachments.size()))
    attachments.foreach(attachment => downloadAttachmentContent(issue, attachment))

    count += 1
    printlog(Messages("message.execute_redmine_issue_export", project.getName, count, allCount))
  }

  private def downloadAttachmentContent(searchIssue: Issue, attachment: Attachment) = {
    val dir: String = ConfigBase.Redmine.getIssueAttachmentDir(project.getIdentifier, searchIssue.getId, attachment.getId)
    IOUtil.createDirectory(dir)
    redmineService.downloadAttachmentContent(attachment, dir)
  }

}

object IssuesActor {

  case class Do()

  def actorName = s"TopIssueActor_$randomUUID"

}
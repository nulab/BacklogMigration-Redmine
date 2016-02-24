package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{AttachmentDownloader, RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Issue, Project, User}

/**
  * @author uchida
  */
class IssuesActor(conf: R2BConfig, project: Project) extends Actor with R2BLogging {

  private val redmineService: RedmineService = new RedmineService(conf)

  private var allCount: Int = 0
  private var count: Int = 0

  def receive: Receive = {
    case IssuesActor.Do =>
      allCount = redmineService.getIssuesCount(project.getId)

      if (allCount != 0) info(Messages("message.execute_redmine_issues_export", project.getName, allCount))

      loop(0)
      context.stop(self)
  }

  private def loop(offset: Int): Unit =
    if (offset < allCount) {
      search(offset)
      loop(offset + Redmine.ISSUE_GET_LIMIT)
    }

  private def search(offset: Int) = {
    val params: Map[String, String] = Map("offset" -> offset.toString, "limit" -> Redmine.ISSUE_GET_LIMIT.toString, "project_id" -> project.getId.toString, "status_id" -> "*", "subproject_id" -> "!*")
    val issues: Seq[Issue] = redmineService.getIssues(params)
    issues.foreach(output)
  }

  private def output(searchIssue: Issue) = {
    val issue: Issue = redmineService.getIssueById(searchIssue.getId, Include.attachments, Include.journals)
    val users: Seq[User] = redmineService.getUsers

    IOUtil.output(ConfigBase.Redmine.getIssuePath(project.getIdentifier, searchIssue.getId), RedmineMarshaller.Issue(issue, project, users))
    AttachmentDownloader.issue(conf.redmineKey, project.getIdentifier, issue)

    count += 1
    info(Messages("message.execute_redmine_issue_export", project.getName, count, allCount))
  }

}

object IssuesActor {

  case class Do()

  def actorName = s"TopIssueActor_$randomUUID"

}
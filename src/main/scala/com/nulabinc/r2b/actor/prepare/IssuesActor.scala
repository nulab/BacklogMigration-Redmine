package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Issue, Journal, Project, User}

import scala.collection.JavaConverters._
import scala.collection.mutable.Set

/**
 * @author uchida
 */
class IssuesActor(r2bConf: R2BConfig, project: Project) extends Actor with R2BLogging {

  private val users = Set.empty[Option[User]]
  private val redmineService: RedmineService = new RedmineService(r2bConf)

  private var count: Int = 0
  private var allCount: Int = 0

  def receive: Receive = {
    case IssuesActor.Do =>
      allCount = redmineService.getIssuesCount(project.getId)
      loop(0)
      sender ! users.flatten
  }

  private def loop(n: Int): Unit = {
    if (n < allCount) {
      searchIssues(n)
      loop(n + Redmine.ISSUE_GET_LIMIT)
    }
  }

  private def searchIssues(offset: Int) = {
    val params: Map[String, String] = Map("offset" -> offset.toString, "limit" -> Redmine.ISSUE_GET_LIMIT.toString, "project_id" -> project.getId.toString, "status_id" -> "*")
    val issues: Seq[Issue] = redmineService.getIssues(params)
    issues.foreach(collectUsers)
  }

  private def collectUsers(searchIssue: Issue) = {
    val issue: Issue = redmineService.getIssueById(searchIssue.getId, Include.journals)
    users += Option(issue.getAssignee)
    users += Option(issue.getAuthor)

    val journals: Array[Journal] = issue.getJournals.toArray(new Array[Journal](issue.getJournals.size()))
    journals.foreach(journal => {
      users += Option(journal.getUser)
      journal.getDetails.asScala.foreach(detail => {
        if (detail.getName == ConfigBase.Property.Attr.ASSIGNED) {
          collectUserFromValue(detail.getOldValue)
          collectUserFromValue(detail.getNewValue)
        }
      })
    })

    count += 1
    info("-  " + Messages("message.load_redmine_issue", project.getName, count, allCount))
  }

  private def collectUserFromValue(value: String) =
    if (Option(value).isDefined && !users.flatten.exists(user => user.getId == value.toInt))
      users += Option(redmineService.getUserById(value.toInt))

}

object IssuesActor {

  case class Do()

  def actorName = s"IssuesActor_$randomUUID"

}
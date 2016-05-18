package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean._

import scala.collection.JavaConverters._
import scala.collection.mutable.Set

/**
  * @author uchida
  */
class IssuesActor(conf: R2BConfig, project: Project) extends Actor with R2BLogging {

  private val users = Set.empty[Option[User]]
  private val redmineService: RedmineService = new RedmineService(conf)
  private val allCount = redmineService.getIssuesCount(project.getId)

  private var count: Int = 0

  def receive: Receive = {
    case IssuesActor.Do =>

      def loop(n: Int): Unit = {
        if (n < allCount) {
          parseIssues(n)
          loop(n + Redmine.ISSUE_GET_LIMIT)
        }
      }

      loop(0)
      sender ! users.flatten
  }

  private def parseIssues(offset: Int) = {
    val params = Map("offset" -> offset.toString, "limit" -> Redmine.ISSUE_GET_LIMIT.toString, "project_id" -> project.getId.toString, "status_id" -> "*", "subproject_id" -> "!*")
    val issues = redmineService.getIssues(params)
    issues.foreach(parseIssue)
  }

  private def parseIssue(searchIssue: Issue) = {
    val issue: Issue = redmineService.getIssueById(searchIssue.getId, Include.journals)
    users += Option(issue.getAssignee)
    users += Option(issue.getAuthor)

    val journals = issue.getJournals.toArray(new Array[Journal](issue.getJournals.size()))
    journals.foreach(parseJournal)

    count += 1
    info("-  " + Messages("message.load_redmine_issue", project.getName, count, allCount))
  }

  private def parseJournal(journal: Journal) = {
    users += Option(journal.getUser)
    journal.getDetails.asScala.foreach(parseJournalDetail)
  }

  private def parseJournalDetail(detail: JournalDetail) = {
    if (detail.getName == ConfigBase.Property.Attr.ASSIGNED) {
      addUser(detail.getOldValue)
      addUser(detail.getNewValue)
    }
  }

  private def addUser(value: String) =
    if (Option(value).isDefined && !users.flatten.exists(_.getId == value.toInt))
      users += redmineService.getUserById(value.toInt)

}

object IssuesActor {

  case class Do()

  def actorName = s"IssuesActor_$randomUUID"

}
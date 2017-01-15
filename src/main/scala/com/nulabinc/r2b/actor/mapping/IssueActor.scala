package com.nulabinc.r2b.actor.mapping

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineProperty
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.IssueService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Issue, Journal, JournalDetail, User}

import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * @author uchida
  */
class IssueActor(issueService: IssueService, mappingData: MappingData, allUsers: Seq[User]) extends Actor with Logging {

  private[this] val users = mutable.Set.empty[Option[User]]
  private[this] val statuses = mutable.Set.empty[Option[String]]

  def receive: Receive = {
    case IssueActor.Do(issueId: Int, completion: CountDownLatch, allCount: Int) =>
      issueService.issueOfId(issueId).fold(
        e => throw e,
        issue => {
          parse(issue)
          mappingData.users ++= users.flatten
        }
      )
      completion.countDown()
      log.info(showMessage(LOG_List, Messages("cli.load_redmine_issue", allCount - completion.getCount, allCount)))
  }

  private[this] def parse(issue: Issue): Unit = {
    users += Option(issue.getAssignee)
    users += Option(issue.getAuthor)

    issue.getJournals.asScala.foreach(parse)
  }

  private[this] def parse(journal: Journal): Unit = {
    users += Option(journal.getUser)
    journal.getDetails.asScala.foreach(parse)
  }

  private[this] def parse(detail: JournalDetail): Unit = {
    if (detail.getName == RedmineProperty.Attr.ASSIGNED) {
      addUser(detail.getOldValue)
      addUser(detail.getNewValue)
    }
    if (detail.getName == RedmineProperty.Attr.STATUS) {
      addStatus(detail.getOldValue)
      addStatus(detail.getNewValue)
    }
  }

  private[this] def addUser(value: String) =
    for {userId <- Option(value)} yield {
      //TODO
      users += allUsers.find(user => user.getId == userId.toInt)
    }

  private[this] def addStatus(value: String) = statuses += Option(value)

}

object IssueActor {

  case class Do(issueId: Int, completion: CountDownLatch, allCount: Int)

}
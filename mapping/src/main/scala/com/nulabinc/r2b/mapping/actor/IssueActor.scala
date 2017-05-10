package com.nulabinc.r2b.mapping.actor

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.r2b.mapping.core.MappingData
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.service.IssueService
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Issue, Journal, JournalDetail, User}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author uchida
  */
class IssueActor(issueService: IssueService, mappingData: MappingData, allUsers: Seq[User]) extends Actor with Logging {

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    logger.debug(s"preRestart: reason: ${reason}, message: ${message}")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(10.seconds, self, value)
    }
  }

  private[this] val users    = mutable.Set.empty[Option[User]]
  private[this] val statuses = mutable.Set.empty[Option[String]]

  def receive: Receive = {
    case IssueActor.Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit)) =>
      logger.debug(s"[START ISSUE]${issueId} thread numbers:${java.lang.Thread.activeCount()}")
      val issue = issueService.issueOfId(issueId, Include.journals)
      parse(issue)
      mappingData.users ++= users.flatten

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
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
    if (detail.getName == RedmineConstantValue.Attr.ASSIGNED) {
      addUser(detail.getOldValue)
      addUser(detail.getNewValue)
    }
    if (detail.getName == RedmineConstantValue.Attr.STATUS) {
      addStatus(detail.getOldValue)
      addStatus(detail.getNewValue)
    }
  }

  private[this] def addUser(value: String) =
    for { userId <- Option(value) } yield users += allUsers.find(user => user.getId.intValue() == userId.toInt)

  private[this] def addStatus(value: String) = statuses += Option(value)

}

object IssueActor {

  case class Do(issueId: Int, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit))

}

package com.nulabinc.r2b.exporter.actor

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.conf.{BacklogConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.{Logging, ProgressBar}
import com.nulabinc.r2b.exporter.convert.{IssueWrites, JournalWrites, UserWrites}
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.service.{IssueService, UserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.Project

import scala.concurrent.duration._

/**
  * @author uchida
  */
class IssuesActor @Inject()(apiConfig: RedmineConfig,
                            backlogPaths: BacklogPaths,
                            issueWrites: IssueWrites,
                            journalWrites: JournalWrites,
                            userWrites: UserWrites,
                            @Named("projectId") projectId: Int,
                            issueService: IssueService)
    extends Actor
    with BacklogConfiguration
    with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val limit      = exportLimitAtOnce
  private[this] val allCount   = issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)

  private[this] val console =
    (ProgressBar.progress _)(Messages("common.issues"), Messages("message.exporting"), Messages("message.exported"))
  private[this] val issuesInfoProgress =
    (ProgressBar.progress _)(Messages("common.issues_info"), Messages("message.collecting"), Messages("message.collected"))

  def receive: Receive = {
    case IssuesActor.Do =>
      val router = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val issueActor =
        context.actorOf(router.props(Props(new IssueActor(apiConfig, backlogPaths, issueService, issueWrites, journalWrites, userWrites))))

      (0 until (allCount, limit))
        .foldLeft(Seq.empty[Int]) { (acc, offset) =>
          acc union issueIds(offset)
        }
        .map(issues)
        .foreach(_(issueActor))

      completion.await
      sender() ! IssuesActor.Done
  }

  private[this] def issueIds(offset: Int): Seq[Int] = {
    val params =
      Map("offset" -> offset.toString, "limit" -> limit.toString, "project_id" -> projectId.toString, "status_id" -> "*", "subproject_id" -> "!*")
    val ids = issueService.allIssues(params).map(_.getId.intValue())
    issuesInfoProgress(((offset / limit) + 1), ((allCount / limit) + 1))
    ids
  }

  private[this] def issues(issueId: Int)(issueActor: ActorRef) = {
    issueActor ! IssueActor.Do(issueId, completion, allCount, console)
  }

}

object IssuesActor extends NamedActor {

  override final val name = "IssuesActor"

  case object Do

  case object Done

}

package com.nulabinc.backlog.r2b.exporter.actor

import java.util.concurrent.CountDownLatch

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.domain.BacklogTextFormattingRule
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.osinka.i18n.Messages

import scala.concurrent.duration._

/**
  * @author uchida
  */
private[exporter] class IssuesActor(exportContext: ExportContext, backlogTextFormattingRule: BacklogTextFormattingRule) extends Actor with BacklogConfiguration with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
    case _ => Restart
  }

  private[this] val limit      = exportLimitAtOnce
  private[this] val allCount   = exportContext.issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)

  private[this] val console =
    (ProgressBar.progress _)(Messages("common.issues"), Messages("message.exporting"), Messages("message.exported"))
  private[this] val issuesInfoProgress =
    (ProgressBar.progress _)(Messages("common.issues_info"), Messages("message.collecting"), Messages("message.collected"))

  def receive: Receive = {
    case IssuesActor.Do =>
      val router     = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val issueActor = context.actorOf(router.props(Props(new IssueActor(exportContext, backlogTextFormattingRule))))

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
      Map("offset"        -> offset.toString,
          "limit"         -> limit.toString,
          "project_id"    -> exportContext.projectId.value.toString,
          "status_id"     -> "*",
          "subproject_id" -> "!*")
    val ids = exportContext.issueService.allIssues(params).map(_.getId.intValue())
    issuesInfoProgress(((offset / limit) + 1), ((allCount / limit) + 1))
    ids
  }

  private[this] def issues(issueId: Int)(issueActor: ActorRef) = {
    issueActor ! IssueActor.Do(issueId, completion, allCount, console)
  }

}

private[exporter] object IssuesActor {

  val name = "IssuesActor"

  case object Do

  case object Done

}

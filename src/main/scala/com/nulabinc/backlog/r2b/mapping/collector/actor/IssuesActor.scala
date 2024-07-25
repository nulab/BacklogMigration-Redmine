package com.nulabinc.backlog.r2b.mapping.collector.actor

import java.util.concurrent.CountDownLatch

import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging, ProgressBar}
import com.nulabinc.backlog.r2b.mapping.collector.core.{MappingContext, MappingData}
import com.nulabinc.backlog4j.BacklogAPIException
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.pekko.actor.SupervisorStrategy.Restart
import org.apache.pekko.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import org.apache.pekko.routing.SmallestMailboxPool

import scala.concurrent.duration._

/**
 * @author
 *   uchida
 */
private[collector] class IssuesActor(mappingContext: MappingContext)(implicit
    s: Scheduler,
    consoleDSL: ConsoleDSL[Task]
) extends Actor
    with BacklogConfiguration
    with Logging {

  private[this] val strategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
      case e: BacklogAPIException if e.getMessage.contains("429") =>
        Restart
      case e: BacklogAPIException if e.getMessage.contains("Stream closed") =>
        Restart
      case e =>
        ConsoleOut.error("Fatal error: " + e.getMessage)
        logger.error(e.getStackTrace.mkString("\n"))
        sys.exit(2)
    }

  private[this] val limit: Int = exportLimitAtOnce
  private[this] val allCount   = mappingContext.issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)
  private[this] val console =
    (ProgressBar.progress _)(
      Messages("common.issues"),
      Messages("message.analyzing"),
      Messages("message.analyzed")
    )
  private[this] val issuesInfoProgress =
    (ProgressBar.progress _)(
      Messages("common.issues_info"),
      Messages("message.collecting"),
      Messages("message.collected")
    )

  def receive: Receive = {
    case IssuesActor.Do(mappingData: MappingData, allUsers: Seq[User]) =>
      val router =
        SmallestMailboxPool(pekkoMailBoxPool, supervisorStrategy = strategy)
      val issueActor = context.actorOf(
        router.props(
          Props(
            new IssueActor(mappingContext.issueService, mappingData, allUsers)
          )
        )
      )

      (0 until (allCount, limit))
        .foldLeft(Seq.empty[Int]) { (acc, offset) =>
          acc concat issueIds(offset)
        }
        .map(issues)
        .foreach(_(issueActor))

      completion.await
      sender() ! IssuesActor.Done
  }

  private[this] def issueIds(offset: Int): Seq[Int] = {
    val params =
      Map(
        "offset"        -> offset.toString,
        "limit"         -> limit.toString,
        "project_id"    -> mappingContext.projectId.value.toString,
        "status_id"     -> "*",
        "subproject_id" -> "!*"
      )
    val ids =
      mappingContext.issueService.allIssues(params).map(_.getId.intValue())
    issuesInfoProgress(((offset / limit) + 1), ((allCount / limit) + 1))
    ids
  }

  private[this] def issues(issueId: Int)(issueActor: ActorRef) = {
    issueActor ! IssueActor.Do(issueId, completion, allCount, console)
  }

}

private[collector] object IssuesActor {

  case class Do(mappingData: MappingData, allUsers: Seq[User])

  case object Done

}

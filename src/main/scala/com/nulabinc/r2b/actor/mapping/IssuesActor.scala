package com.nulabinc.r2b.actor.mapping

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.IssueService
import com.taskadapter.redmineapi.bean.{Issue, User}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * @author uchida
  */
class IssuesActor @Inject()(@Named("projectId") projectId: Int, issueService: IssueService) extends Actor with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val limit = ConfigFactory.load().getInt("application.export.issue-get-limit")
  private[this] val allCount = issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)

  def receive: Receive = {
    case IssuesActor.Do(mappingData: MappingData, allUsers: Seq[User]) =>

      val router = SmallestMailboxPool(ConfigFactory.load().getInt("akka.mailbox-pool"), supervisorStrategy = strategy)
      val issueActor = context.actorOf(router.props(Props(new IssueActor(issueService, mappingData, allUsers))))

      def loop(offset: Long): Unit = {
        if (offset < allCount) {
          issues(issueActor, offset)
          loop(offset + limit)
        }
      }

      loop(0)

      completion.await
      context.stop(self)
  }

  private[this] def issues(issueActor: ActorRef, offset: Long) = {
    val params = Map("offset" -> offset.toString, "limit" -> limit.toString, "project_id" -> projectId.toString, "status_id" -> "*", "subproject_id" -> "!*")
    val issues: Seq[Issue] = issueService.allIssues(params)
    issues.foreach(issue => issueActor ! IssueActor.Do(issue.getId, completion, allCount))
  }

}

object IssuesActor extends NamedActor {

  case class Do(mappingData: MappingData, allUsers: Seq[User])

  override final val name = "IssuesActor"

}
package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service.{AttachmentDownloadService, IssueService, UserService}
import com.taskadapter.redmineapi.bean.{Issue, Project, User}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * @author uchida
  */
class IssuesActor @Inject()(
                             redmineDirectory: RedmineDirectory,
                             @Named("key") key: String,
                             @Named("projectId") projectId: Int,
                             project: Project,
                             attachmentDownloadService: AttachmentDownloadService,
                             userService: UserService,
                             issueService: IssueService) extends Actor with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val limit = ConfigFactory.load().getInt("application.export.issue-get-limit")
  private[this] val allCount = issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)

  def receive: Receive = {
    case IssuesActor.Do =>
      val users: Seq[User] = userService.allUsers()

      val router = SmallestMailboxPool(ConfigFactory.load().getInt("akka.mailbox-pool"), supervisorStrategy = strategy)
      val issueActor = context.actorOf(router.props(Props(new IssueActor(redmineDirectory, key, project, attachmentDownloadService, issueService, users))))

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

  case object Do

  override final val name = "IssuesActor"

}
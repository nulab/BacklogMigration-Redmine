package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.conf.CommonConfigBase
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.{AppConfiguration, RedmineDirectory}
import com.nulabinc.r2b.service.{AttachmentDownloadService, IssueService, UserService}
import com.taskadapter.redmineapi.bean.{Issue, Project, User}


/**
  * @author uchida
  */
class IssuesActor @Inject()(
                             redmineDirectory: RedmineDirectory,
                             @Named("key") key: String,
                             @Named("projectKey") projectKey: String,
                             @Named("projectId") projectId: Int,
                             project: Project,
                             config: AppConfiguration,
                             attachmentDownloadService: AttachmentDownloadService,
                             userService: UserService,
                             issueService: IssueService) extends Actor with Logging {

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case _ =>
        Escalate
    }
    OneForOneStrategy()(decider orElse super.supervisorStrategy.decider)
  }

  private[this] val allCount = issueService.countIssues()
  private[this] val completion = new CountDownLatch(allCount)

  def receive: Receive = {
    case IssuesActor.Do() =>
      val users: Seq[User] = userService.allUsers()
      val issueActor = context.actorOf(SmallestMailboxPool(CommonConfigBase.ACTOR_POOL_SIZE).
        props(Props(new IssueActor(redmineDirectory, key, projectKey, project, attachmentDownloadService, issueService, users))))

      def loop(offset: Long): Unit = {
        if (offset < allCount) {
          issues(issueActor, offset)
          loop(offset + CommonConfigBase.ISSUE_GET_LIMIT)
        }
      }

      loop(0)

      completion.await
      context.stop(self)
  }

  private[this] def issues(issueActor: ActorRef, offset: Long) = {
    val params = Map("offset" -> offset.toString, "limit" -> CommonConfigBase.ISSUE_GET_LIMIT.toString, "project_id" -> projectId.toString, "status_id" -> "*", "subproject_id" -> "!*")
    val issues: Seq[Issue] = issueService.allIssues(params)
    issues.foreach(issue => issueActor ! IssueActor.Do(issue.getId, completion, allCount))
  }

}

object IssuesActor extends NamedActor {

  case class Do()

  override final val name = "IssuesActor"

}
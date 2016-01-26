package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean._

import scala.concurrent.duration._
import scala.language.postfixOps

class RedmineActor(r2bConf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case e: Exception =>
      error(e)
      context.system.shutdown()
      Stop
  }

  val redmineService: RedmineService = new RedmineService(r2bConf)

  def receive: Receive = {
    case RedmineActor.Do =>
      title(Messages("message.start_redmine_export"), TOP)

      users()
      customFields()
      trackers()
      issueStatuses()
      priorities()
      start(Props(new ProjectsActor(r2bConf)), ProjectsActor.actorName) ! ProjectsActor.Do
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) {
        title(Messages("message.completed_redmine_export"), BOTTOM)
        newLine()
        newLine()
        context.system.shutdown()
      }
  }

  private def users() = {
    info(Messages("message.execute_redmine_user_export"))
    val users: Seq[User] = redmineService.getUsers
    IOUtil.output(ConfigBase.Redmine.USERS, RedmineMarshaller.Users(users))
  }

  private def customFields() = {
    info(Messages("message.execute_redmine_custom_fields_export"))
    val redmineService: RedmineService = new RedmineService(r2bConf)
    val either: Either[Throwable, Seq[CustomFieldDefinition]] = redmineService.getCustomFieldDefinitions
    either match {
      case Right(customFieldDefinitions) =>
        IOUtil.output(Redmine.CUSTOM_FIELDS, RedmineMarshaller.CustomFieldDefinition(customFieldDefinitions))
      case Left(e) =>
    }
  }

  private def trackers() = {
    info(Messages("message.execute_redmine_issue_trackers_export"))
    val redmineService: RedmineService = new RedmineService(r2bConf)
    val either: Either[Throwable, Seq[Tracker]] = redmineService.getTrackers
    either match {
      case Right(trackers) =>
        IOUtil.output(ConfigBase.Redmine.TRACKERS, RedmineMarshaller.Tracker(trackers))
      case Left(e) =>
    }
  }

  private def issueStatuses() = {
    info(Messages("message.execute_redmine_issue_statuses_export"))
    val redmineService: RedmineService = new RedmineService(r2bConf)
    val either: Either[Throwable, Seq[IssueStatus]] = redmineService.getStatuses
    either match {
      case Right(issueStatuses) =>
        IOUtil.output(ConfigBase.Redmine.ISSUE_STATUSES, RedmineMarshaller.IssueStatus(issueStatuses))
      case Left(e) =>
    }
  }

  private def priorities() = {
    val redmineService: RedmineService = new RedmineService(r2bConf)
    val either: Either[Throwable, Seq[IssuePriority]] = redmineService.getIssuePriorities
    either match {
      case Right(issuePriorities) =>
        IOUtil.output(ConfigBase.Redmine.PRIORITY, RedmineMarshaller.IssuePriority(issuePriorities))
      case Left(e) =>
    }
  }

}

object RedmineActor {
  implicit val timeout = Timeout(60 minutes)

  case class Do()

  def actorName = s"RedmineActor_$randomUUID"

  def apply(r2bConf: R2BConfig) = {
    val system = ActorSystem("redmine-exporter")
    val actor = system.actorOf(Props(new RedmineActor(r2bConf)), RedmineActor.actorName)
    actor ! RedmineActor.Do
    system.awaitTermination(timeout.duration)
  }

}
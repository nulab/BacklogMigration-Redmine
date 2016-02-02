package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{CustomFieldConverter, RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages

import scala.concurrent.duration._
import scala.language.postfixOps

class RedmineActor(conf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case e: Exception =>
      error(e)
      context.system.shutdown()
      Stop
  }

  val redmineService: RedmineService = new RedmineService(conf)

  def receive: Receive = {
    case RedmineActor.Do =>
      title(Messages("message.start_redmine_export"), TOP)

      info(Messages("message.execute_redmine_user_export"))
      IOUtil.output(ConfigBase.Redmine.USERS, RedmineMarshaller.Users(redmineService.getUsers))

      info(Messages("message.execute_redmine_custom_fields_export"))
      val customFieldConverter = new CustomFieldConverter(conf)
      IOUtil.output(Redmine.CUSTOM_FIELDS, RedmineMarshaller.CustomFieldDefinition(customFieldConverter.execute(redmineService.getCustomFieldDefinitions)))

      info(Messages("message.execute_redmine_issue_trackers_export"))
      IOUtil.output(ConfigBase.Redmine.TRACKERS, RedmineMarshaller.Tracker(redmineService.getTrackers))

      info(Messages("message.execute_redmine_issue_statuses_export"))
      IOUtil.output(ConfigBase.Redmine.ISSUE_STATUSES, RedmineMarshaller.IssueStatus(redmineService.getStatuses))

      IOUtil.output(ConfigBase.Redmine.PRIORITY, RedmineMarshaller.IssuePriority(redmineService.getIssuePriorities()))

      start(Props(new ProjectsActor(conf)), ProjectsActor.actorName) ! ProjectsActor.Do

    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) {
        title(Messages("message.completed_redmine_export"), BOTTOM)
        newLine()
        newLine()
        context.system.shutdown()
      }
  }

}

object RedmineActor {
  implicit val timeout = Timeout(60 minutes)

  case class Do()

  def actorName = s"RedmineActor_$randomUUID"

  def apply(conf: R2BConfig) = {
    val system = ActorSystem("redmine-exporter")
    val actor = system.actorOf(Props(new RedmineActor(conf)), RedmineActor.actorName)
    actor ! RedmineActor.Do
    system.awaitTermination(timeout.duration)
  }

}
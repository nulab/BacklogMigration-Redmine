package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.{R2BConfig, ConfigBase}
import com.nulabinc.r2b.service.{RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean._

/**
 * @author uchida
 */
class ProjectsActor(conf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  val redmineService: RedmineService = new RedmineService(conf)

  def receive: Receive = {
    case ProjectsActor.Do =>
      info(Messages("message.start_redmine_projects_export"))

      val projects: Seq[Project] = redmineService.getProjects
      IOUtil.output(ConfigBase.Redmine.PROJECTS, RedmineMarshaller.Projects(projects))

      if (projects.nonEmpty) projects.foreach(contents)
      else context.stop(self)
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.stop(self)
  }

  private def contents(project: Project) = {
    info(Messages("message.execute_redmine_project_export", project.getName))

    info(Messages("message.execute_redmine_memberships_export", project.getName))
    val memberships = redmineService.getMemberships(project.getIdentifier)
    IOUtil.output(Redmine.getMembershipsPath(project.getIdentifier), RedmineMarshaller.Membership(memberships))

    val groups:Seq[Group] = memberships.flatMap(membership => Option(membership.getGroup))
    IOUtil.output(ConfigBase.Redmine.GROUP_USERS, RedmineMarshaller.Group(groups))

    info(Messages("message.execute_redmine_issue_categories_export", project.getName))
    IOUtil.output(Redmine.getIssueCategoriesPath(project.getIdentifier), RedmineMarshaller.IssueCategory(redmineService.getCategories(project.getId)))

    info(Messages("message.execute_redmine_versions_export", project.getName))
    IOUtil.output(Redmine.getVersionsPath(project.getIdentifier), RedmineMarshaller.Versions(redmineService.getVersions(project.getId)))

    start(Props(new NewsActor(conf, project)), NewsActor.actorName) ! NewsActor.Do
    start(Props(new WikisActor(conf, project)), WikisActor.actorName) ! WikisActor.Do
    start(Props(new IssuesActor(conf, project)), IssuesActor.actorName) ! IssuesActor.Do
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
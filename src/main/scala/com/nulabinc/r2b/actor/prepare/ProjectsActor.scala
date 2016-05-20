package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Group, Membership, Project, User}

/**
  * @author uchida
  */
class ProjectsActor(conf: R2BConfig, prepareData: PrepareData) extends Actor with R2BLogging with Subtasks {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  private val redmineService: RedmineService = new RedmineService(conf)

  def receive: Receive = {
    case ProjectsActor.Do =>
      info("-  " + Messages("message.load_redmine_projects"))
      redmineService.getProjects.foreach(parseProject)
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.stop(self)
  }

  private def parseProject(project: Project) = {

    start(Props(new IssuesActor(conf, project, prepareData)), IssuesActor.actorName) ! IssuesActor.Do

    start(Props(new WikisActor(conf, project, prepareData)), WikisActor.actorName) ! WikisActor.Do

    info("-  " + Messages("message.load_redmine_memberships", project.getName))
    val memberships = redmineService.getMemberships(project.getIdentifier)
    memberships.foreach(parseMembership)
  }

  private def parseMembership(membership: Membership) = {
    if (Option(membership.getUser).isDefined) prepareData.users += membership.getUser
    if (Option(membership.getGroup).isDefined) redmineService.getUsers.foreach(parseUserGroups(membership.getGroup))
  }

  private def parseUserGroups(group: Group)(user: User) = {
    val userGroups = user.getGroups.toArray(new Array[Group](user.getGroups.size()))
    userGroups.foreach(userGroup => if (group.getId == userGroup.getId) prepareData.users += user)
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
package com.nulabinc.r2b.actor.prepare

import java.util.UUID._
import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Group, Membership, Project, User}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author uchida
  */
class ProjectsActor(conf: R2BConfig) extends Actor with R2BLogging {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  implicit val timeout: Timeout = Timeout(ConfigFactory.load().getDuration("r2b.prepare", TimeUnit.MINUTES), TimeUnit.MINUTES)

  private val redmineService: RedmineService = new RedmineService(conf)
  private val projects: Seq[Project] = redmineService.getProjects
  private val users: Set[User] = Set.empty[User]

  def receive: Receive = {
    case ProjectsActor.Do =>
      info("-  " + Messages("message.load_redmine_projects"))

      val s = sender

      projects.foreach(parseProject)

      val futures = projects.foldLeft(Seq.empty[Future[Set[User]]])((fs, project) => {
        fs :+ parseIssue(project, s) :+ parseWiki(project, s)
      })
      val f = Future.fold(futures)(Set.empty[User])((total, users) => total ++= users)

      s ! (users ++= Await.result(f, Duration.Inf))
  }

  private def parseIssue(project: Project, caller: ActorRef): Future[Set[User]] = {
    val actor = context.actorOf(Props(new IssuesActor(conf, project)), IssuesActor.actorName)
    (actor ? IssuesActor.Do).mapTo[Set[User]]
  }

  private def parseWiki(project: Project, caller: ActorRef): Future[Set[User]] = {
    val actor = context.actorOf(Props(new WikisActor(conf, project)), WikisActor.actorName)
    (actor ? WikisActor.Do).mapTo[Set[User]]
  }

  private def parseProject(project: Project) = {
    info("-  " + Messages("message.load_redmine_memberships", project.getName))
    val memberships = redmineService.getMemberships(project.getIdentifier)
    memberships.foreach(parseMembership)
  }

  private def parseMembership(membership: Membership) = {
    if (Option(membership.getUser).isDefined) users += membership.getUser
    if (Option(membership.getGroup).isDefined) redmineService.getUsers.foreach(parseUserGroups(membership.getGroup))
  }

  private def parseUserGroups(group: Group)(user: User) = {
    val userGroups = user.getGroups.toArray(new Array[Group](user.getGroups.size()))
    userGroups.foreach(userGroup => if (group.getId == userGroup.getId) users += user)
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
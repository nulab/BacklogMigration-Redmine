package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Group, Project, User}

import scala.collection.mutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * @author uchida
 */
class ProjectsActor(r2bConf: R2BConfig) extends Actor with R2BLogging {

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  implicit val timeout = Timeout(60 minutes)
  private val redmineService: RedmineService = new RedmineService(r2bConf)
  private val allUsers: Seq[User] = redmineService.getUsers

  def receive: Receive = {
    case ProjectsActor.Do =>
      val caller = sender
      printlog("-  " + Messages("message.load_redmine_projects"))
      val projects: Seq[Project] = redmineService.getProjects

      val futures: Seq[Future[Set[User]]] = projects.foldLeft(Seq.empty[Future[Set[User]]])((fs: Seq[Future[Set[User]]], project: Project) => {
        fs :+ searchFromIssue(project, caller) :+ searchFromWiki(project, caller)
      })

      val f: Future[Set[User]] = Future.fold(futures)(Set.empty[User])((total: Set[User], users: Set[User]) => total ++= users)

      val users: Set[User] = Await.result(f, Duration.Inf)
      users ++= memberships(projects)
      caller ! users
      context.stop(self)
  }

  private def searchFromIssue(project: Project, caller: ActorRef): Future[Set[User]] = {
    val actor = context.actorOf(Props(new IssuesActor(r2bConf, project)), IssuesActor.actorName)
    (actor ? IssuesActor.Do).mapTo[Set[User]]
  }

  private def searchFromWiki(project: Project, caller: ActorRef): Future[Set[User]] = {
    val actor = context.actorOf(Props(new WikisActor(r2bConf, project)), WikisActor.actorName)
    (actor ? WikisActor.Do).mapTo[Set[User]]
  }

  private def memberships(projects: Seq[Project]): Set[User] = {
    val users: Set[User] = Set.empty[User]
    projects.foreach(project => {
      printlog("-  " + Messages("message.load_redmine_memberships", project.getName))
      redmineService.getMemberships(project.getIdentifier) match {
        case Right(memberships) => memberships.foreach(membership => {
          if (Option(membership.getUser).isDefined) users += membership.getUser
          if (Option(membership.getGroup).isDefined) users ++= groups(membership.getGroup)
        })
        case Left(e) => log.debug(e.getMessage)
      }
    })
    users
  }

  private def groups(group: Group): Set[User] = {
    val users: Set[User] = Set.empty[User]
    allUsers.foreach(user => {
      val userGroups: Array[Group] = user.getGroups.toArray(new Array[Group](user.getGroups.size()))
      userGroups.foreach(userGroup => {
        if (group.getId == userGroup.getId) users += user
      })
    })
    users
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
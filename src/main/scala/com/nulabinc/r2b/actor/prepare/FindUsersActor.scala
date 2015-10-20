package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * @author uchida
 */
class FindUsersActor(r2bConf: R2BConfig) extends Actor with R2BLogging {

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case e: Exception =>
      errorLog(e)
      context.system.shutdown()
      Stop
  }

  implicit val timeout = Timeout(60 minutes)

  private val allUsers = Set.empty[User]

  def receive: Receive = {
    case FindUsersActor.Do =>

      val caller = sender()

      val actor = context.actorOf(Props(new ProjectsActor(r2bConf)), ProjectsActor.actorName)

      val f: Future[Set[User]] = (actor ? ProjectsActor.Do).mapTo[Set[User]]

      for {users <- f} yield {
        allUsers ++= users
        caller ! allUsers
        context.stop(self)
      }
  }

}

object FindUsersActor {
  implicit val timeout = Timeout(60 minutes)

  case class Do()

  def actorName = s"FindUsersActor_$randomUUID"

  def apply(r2bConf: R2BConfig): Set[User] = {
    val system = ActorSystem("find-users")
    val actor = system.actorOf(Props(new FindUsersActor(r2bConf)), FindUsersActor.actorName)

    val f: Future[Set[User]] = (actor ? FindUsersActor.Do).mapTo[Set[User]]
    val users: Set[User] = Await.result(f, Duration.Inf)
    users
  }

}

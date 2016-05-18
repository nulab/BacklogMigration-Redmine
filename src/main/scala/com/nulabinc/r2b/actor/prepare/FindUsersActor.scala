package com.nulabinc.r2b.actor.prepare

import java.util.UUID._
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.taskadapter.redmineapi.bean.User
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author uchida
  */
class FindUsersActor(conf: R2BConfig) extends Actor with R2BLogging {

  implicit val timeout = Timeout(ConfigFactory.load().getDuration("r2b.prepare", TimeUnit.MINUTES), TimeUnit.MINUTES)

  def receive: Receive = {
    case FindUsersActor.Do =>

      val caller = sender()

      val actor = context.actorOf(Props(new ProjectsActor(conf)), ProjectsActor.actorName)
      val f = (actor ? ProjectsActor.Do).mapTo[Set[User]]

      for {users <- f} yield {
        caller ! users
        context.stop(self)
      }
  }

}

object FindUsersActor {

  implicit val timeout: Timeout = Timeout(ConfigFactory.load().getDuration("r2b.prepare", TimeUnit.MINUTES), TimeUnit.MINUTES)

  case class Do()

  def actorName = s"FindUsersActor_$randomUUID"

  def apply(conf: R2BConfig): Set[User] = {
    val system = ActorSystem("find-users")
    val actor = system.actorOf(Props(new FindUsersActor(conf)), FindUsersActor.actorName)
    val f = (actor ? FindUsersActor.Do).mapTo[Set[User]]
    Await.result(f, Duration.Inf)
  }

}

package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable.Set
import scala.concurrent.duration._

/**
  * @author uchida
  */
class ParseActor(conf: R2BConfig, prepareData: PrepareData) extends Actor with R2BLogging {

  private val actor = context.watch(context.actorOf(Props(new ProjectsActor(conf, prepareData)), ProjectsActor.actorName))

  def receive: Receive = {
    case ParseActor.Do =>
      actor ! ProjectsActor.Do
    case Terminated(ref) =>
      context.system.shutdown()
  }

}

object ParseActor {

  case class Do()

  def actorName = s"ParseActor_$randomUUID"

  def apply(conf: R2BConfig): PrepareData = {

    val prepareData = PrepareData(Set.empty[User], Set.empty[String])

    val system = ActorSystem("parse")
    val actor = system.actorOf(Props(new ParseActor(conf, prepareData)), ParseActor.actorName)
    actor ! ParseActor.Do
    system.awaitTermination(Duration.Inf)
    prepareData
  }

}

case class PrepareData(users: Set[User], statuses: Set[String])
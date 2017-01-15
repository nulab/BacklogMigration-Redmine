package com.nulabinc.r2b.actor.redmine

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy, Terminated}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.nulabinc.backlog.migration.actor.utils.Subtasks
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging


/**
  * @author uchida
  */
class ContentActor @Inject()(@Named(IssuesActor.name) issuesActor: ActorRef,@Named(WikisActor.name) wikisActor: ActorRef) extends Actor with Subtasks with Logging {

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case e: Throwable =>
        log.error(e.getMessage, e)
        Stop
    }
    OneForOneStrategy()(decider orElse super.supervisorStrategy.decider)
  }

  def receive: Receive = {
    case ContentActor.Do() =>
      start(issuesActor) ! IssuesActor.Do()
      start(wikisActor) ! WikisActor.Do()
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.system.shutdown()
  }

}

object ContentActor extends NamedActor {

  case class Do()

  override final val name = "ContentActor"

}

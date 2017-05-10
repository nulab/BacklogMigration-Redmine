package com.nulabinc.r2b.exporter.actor

import akka.actor.{Actor, ActorRef}
import com.google.inject.Inject
import com.google.inject.name.Named
import com.nulabinc.backlog.migration.common.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.common.utils.Logging

/**
  * @author uchida
  */
class ContentActor @Inject()(@Named(IssuesActor.name) issuesActor: ActorRef, @Named(WikisActor.name) wikisActor: ActorRef)
    extends Actor
    with Logging {

  def receive: Receive = {
    case ContentActor.Do =>
      wikisActor ! WikisActor.Do
    case WikisActor.Done =>
      issuesActor ! IssuesActor.Do
    case IssuesActor.Done =>
      context.system.shutdown()
  }

}

object ContentActor extends NamedActor {

  override final val name = "ContentActor"

  case object Do

}

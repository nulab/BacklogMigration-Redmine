package com.nulabinc.backlog.r2b.exporter.actor

import akka.actor.{Actor, Props}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.exporter.core.ExportContext

/**
  * @author uchida
  */
private[exporter] class ContentActor(exportContext: ExportContext) extends Actor with Logging {

  private[this] val wikisActor  = context.actorOf(Props(new WikisActor(exportContext)))
  private[this] val issuesActor = context.actorOf(Props(new IssuesActor(exportContext)))

  def receive: Receive = {
    case ContentActor.Do =>
      wikisActor ! WikisActor.Do
    case WikisActor.Done =>
      issuesActor ! IssuesActor.Do
    case IssuesActor.Done =>
      context.system.shutdown()
  }

}

private[exporter] object ContentActor {

  val name = "ContentActor"

  case object Do

}

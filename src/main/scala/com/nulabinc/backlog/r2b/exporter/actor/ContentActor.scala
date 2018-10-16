package com.nulabinc.backlog.r2b.exporter.actor

import akka.actor.{Actor, Props}
import com.nulabinc.backlog.migration.common.domain.BacklogTextFormattingRule
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.exporter.core.ExportContext

/**
  * @author uchida
  */
private[exporter] class ContentActor(exportContext: ExportContext, backlogTextFormattingRule: BacklogTextFormattingRule) extends Actor with Logging {

  private[this] val wikisActor  = context.actorOf(Props(new WikisActor(exportContext)))
  private[this] val issuesActor = context.actorOf(Props(new IssuesActor(exportContext, backlogTextFormattingRule)))

  def receive: Receive = {
    case ContentActor.Do =>
      if (isMigrate(exportContext.exportConfig.exclude, "wiki")) {
        wikisActor ! WikisActor.Do
      } else {
        self ! WikisActor.Done
      }
    case WikisActor.Done =>
      if (isMigrate(exportContext.exportConfig.exclude, "issue")) {
        issuesActor ! IssuesActor.Do
      } else {
        self ! IssuesActor.Done
      }
    case IssuesActor.Done =>
      context.system.terminate()
  }

  private[this] def isMigrate(exclude: Option[List[String]], item: String): Boolean =
    exclude match {
      case Some(excludes) => !excludes.contains(item)
      case _              => true
    }
}

private[exporter] object ContentActor {

  val name = "ContentActor"

  case object Do

}

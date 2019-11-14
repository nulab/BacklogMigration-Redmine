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
      if (exportContext.exportConfig.exclude.excludeWiki) {
        self ! WikisActor.Done
      } else {
        wikisActor ! WikisActor.Do
      }
    case WikisActor.Done =>
      if (exportContext.exportConfig.exclude.excludeIssue) {
        self ! IssuesActor.Done
      } else {
        issuesActor ! IssuesActor.Do
      }
    case IssuesActor.Done =>
      context.system.terminate()
  }

}

private[exporter] object ContentActor {

  val name = "ContentActor"

  case object Do

}

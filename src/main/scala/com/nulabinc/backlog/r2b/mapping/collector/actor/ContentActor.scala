package com.nulabinc.backlog.r2b.mapping.collector.actor

import akka.actor.{Actor, Props}
import com.nulabinc.backlog.migration.common.conf.ExcludeOption
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.mapping.collector.core.{MappingContext, MappingData}
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
private[collector] class ContentActor(exclude: ExcludeOption, mappingContext: MappingContext)
    extends Actor
    with Logging {

  private[this] val wikisActor  = context.actorOf(Props(new WikisActor(mappingContext)))
  private[this] val issuesActor = context.actorOf(Props(new IssuesActor(mappingContext)))

  def receive: Receive = {
    case ContentActor.Do(mappingData: MappingData) =>
      if (exclude.wiki) {
        self ! WikisActor.Done(mappingData)
      } else {
        wikisActor ! WikisActor.Do(mappingData)
      }
    case WikisActor.Done(mappingData) =>
      val allUsers: Seq[User] = mappingContext.userService.allUsers()
      issuesActor ! IssuesActor.Do(mappingData, allUsers)
    case IssuesActor.Done =>
      context.system.terminate()
  }

}

private[collector] object ContentActor {

  val name = "ContentActor"

  case class Do(mappingData: MappingData)

}

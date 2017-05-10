package com.nulabinc.backlog.r2b.mapping.actor

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorRef}
import com.nulabinc.backlog.migration.common.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.mapping.core.MappingData
import com.nulabinc.backlog.r2b.redmine.service.UserService
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
class ContentActor @Inject()(@Named(IssuesActor.name) issuesActor: ActorRef, @Named(WikisActor.name) wikisActor: ActorRef, userService: UserService)
    extends Actor
    with Logging {

  def receive: Receive = {
    case ContentActor.Do(mappingData: MappingData) =>
      wikisActor ! WikisActor.Do(mappingData)
    case WikisActor.Done(mappingData) =>
      val allUsers: Seq[User] = userService.allUsers()
      issuesActor ! IssuesActor.Do(mappingData, allUsers)
    case IssuesActor.Done =>
      context.system.shutdown()
  }

}

object ContentActor extends NamedActor {

  override final val name = "ContentActor"

  case class Do(mappingData: MappingData)

}

package com.nulabinc.r2b.actor.mapping

import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy, Terminated}
import com.nulabinc.backlog.migration.actor.utils.Subtasks
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.{IssueService, UserService}
import com.taskadapter.redmineapi.bean.User


/**
  * @author uchida
  */
class ContentActor @Inject()(
                              config: AppConfiguration,
                              @Named("projectId") projectId: Int,
                              @Named(IssuesActor.name) issuesActor: ActorRef,
                              @Named(WikisActor.name) wikisActor: ActorRef,
                              userService: UserService,
                              issueService: IssueService) extends Actor with Subtasks with Logging {

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case e: Throwable =>
        log.error(e.getMessage, e)
        Stop
    }
    OneForOneStrategy()(decider orElse super.supervisorStrategy.decider)
  }

  def receive: Receive = {
    case ContentActor.Do(mappingData: MappingData) =>

      val allUsers: Seq[User] = userService.allUsers()
      start(issuesActor) ! IssuesActor.Do(mappingData, allUsers)
      start(wikisActor) ! WikisActor.Do(mappingData)

    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.system.shutdown()
  }

}

object ContentActor extends NamedActor {

  case class Do(mappingData: MappingData)

  override final val name = "ContentActor"

}

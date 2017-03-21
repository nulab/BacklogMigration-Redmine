package com.nulabinc.r2b.actor.mapping.service

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Injector
import com.nulabinc.backlog.migration.di.akkaguice.GuiceAkkaExtension
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.actor.mapping.ContentActor
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.{MembershipService, UserService}
import com.taskadapter.redmineapi.bean.{Group, Membership, User}
import net.codingwell.scalaguice.InjectorExtensions._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
class ProjectApplicationService @Inject()(membershipService: MembershipService, userService: UserService) extends Logging {

  def execute(injector: Injector, mappingData: MappingData) = {
    val system = injector.instance[ActorSystem]
    val contentActor = system.actorOf(GuiceAkkaExtension(system).props(ContentActor.name))
    contentActor ! ContentActor.Do(mappingData)
    system.awaitTermination(Duration.Inf)

    val memberships = membershipService.allMemberships()
    memberships.foreach(membership => parse(membership, mappingData))
  }

  private[this] def parse(membership: Membership, mappingData: MappingData): Unit = {
    for {user <- Option(membership.getUser)} yield {
      mappingData.users += user
    }
    for {group <- Option(membership.getGroup)} yield {
      userService.allUsers().foreach(user => parse(group, user, mappingData))
    }
  }

  private[this] def parse(group: Group, user: User, mappingData: MappingData): Unit = {
    user.getGroups.asScala.foreach(userGroup => if (group.getId == userGroup.getId) mappingData.users += user)
  }

}

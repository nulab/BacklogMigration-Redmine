package com.nulabinc.backlog.r2b.mapping.service

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Injector
import com.nulabinc.backlog.migration.common.modules.akkaguice.GuiceAkkaExtension
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.mapping.actor.ContentActor
import com.nulabinc.backlog.r2b.mapping.core.MappingData
import com.nulabinc.backlog.r2b.redmine.service.MembershipService
import com.nulabinc.backlog.r2b.redmine.service.{NewsService, UserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Group, Membership, User}
import net.codingwell.scalaguice.InjectorExtensions._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
class ProjectApplicationService @Inject()(membershipService: MembershipService, userService: UserService, newsService: NewsService) extends Logging {

  def execute(injector: Injector, mappingData: MappingData) = {
    val system       = injector.instance[ActorSystem]
    val contentActor = system.actorOf(GuiceAkkaExtension(system).props(ContentActor.name))
    contentActor ! ContentActor.Do(mappingData)

    system.awaitTermination(Duration.Inf)

    val memberships = membershipService.allMemberships()
    memberships.foreach(membership => parse(membership, mappingData))

    //news
    val console = (ProgressBar.progress _)(Messages("common.news"), Messages("message.analyzing"), Messages("message.analyzed"))
    val allNews = newsService.allNews()
    allNews.zipWithIndex.foreach {
      case (news, index) =>
        mappingData.users += news.getUser
        console(index + 1, allNews.size)
    }
  }

  private[this] def parse(membership: Membership, mappingData: MappingData): Unit = {
    for { user <- Option(membership.getUser) } yield {
      mappingData.users += user
    }
    for { group <- Option(membership.getGroup) } yield {
      userService.allUsers().foreach(user => parse(group, user, mappingData))
    }
  }

  private[this] def parse(group: Group, user: User, mappingData: MappingData): Unit = {
    user.getGroups.asScala.foreach(userGroup => if (group.getId == userGroup.getId) mappingData.users += user)
  }

}

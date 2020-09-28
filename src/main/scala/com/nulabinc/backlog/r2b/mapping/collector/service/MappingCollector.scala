package com.nulabinc.backlog.r2b.mapping.collector.service

import javax.inject.Inject
import akka.actor.{ActorSystem, Props}
import com.nulabinc.backlog.migration.common.conf.ExcludeOption
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.mapping.collector.actor.ContentActor
import com.nulabinc.backlog.r2b.mapping.collector.core.{MappingContextProvider, MappingData}
import com.nulabinc.backlog.r2b.redmine.service.{MembershipService, NewsService, UserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Group, Membership, User}

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
private[collector] class MappingCollector @Inject() (
    mappingContextProvider: MappingContextProvider,
    membershipService: MembershipService,
    userService: UserService,
    newsService: NewsService
) extends Logging {

  def boot(exclude: ExcludeOption, mappingData: MappingData): Unit = {
    val mappingContext = mappingContextProvider.get()
    val system         = ActorSystem.apply("main-actor-system")
    val contentActor   = system.actorOf(Props(new ContentActor(exclude, mappingContext)))
    contentActor ! ContentActor.Do(mappingData)

    Await.result(system.whenTerminated, Duration.Inf)

    val memberships = membershipService.allMemberships()
    memberships.foreach(membership => parse(membership, mappingData))

    //news
    val console = (ProgressBar.progress _)(
      Messages("common.news"),
      Messages("message.analyzing"),
      Messages("message.analyzed")
    )
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
    user.getGroups.asScala.foreach(userGroup =>
      if (group.getId == userGroup.getId) mappingData.users += user
    )
  }

}

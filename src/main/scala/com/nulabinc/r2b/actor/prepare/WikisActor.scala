package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Project, User, WikiPage}

import scala.collection.mutable.Set

/**
  * @author uchida
  */
class WikisActor(conf: R2BConfig, project: Project) extends Actor with R2BLogging {

  private val users = Set.empty[Option[User]]
  private val redmineService: RedmineService = new RedmineService(conf)
  private val wikiPages = redmineService.getWikiPagesByProject(project.getIdentifier)

  private var count = 0

  def receive: Receive = {
    case WikisActor.Do =>
      wikiPages.foreach(parseWikiPage)
      sender ! users.flatten
  }

  private def parseWikiPage(page: WikiPage) = {
    val detail = redmineService.getWikiPageDetailByProjectAndTitle(project.getIdentifier, page.getTitle)
    users += Option(detail.getUser)

    count += 1
    info("-  " + Messages("message.load_redmine_wikis", project.getName, count, wikiPages.size))
  }

}

object WikisActor {

  case class Do()

  def actorName = s"WikisActor_$randomUUID"

}
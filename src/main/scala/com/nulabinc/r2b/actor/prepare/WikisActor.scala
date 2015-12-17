package com.nulabinc.r2b.actor.prepare

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, AllForOneStrategy}
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Project, User, WikiPage, WikiPageDetail}

import scala.collection.mutable.Set

/**
  * @author uchida
  */
class WikisActor(r2bConf: R2BConfig, project: Project) extends Actor with R2BLogging {

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  private var count = 0

  def receive: Receive = {
    case WikisActor.Do =>
      val users = Set.empty[Option[User]]
      val redmineService: RedmineService = new RedmineService(r2bConf)
      redmineService.getWikiPagesByProject(project.getIdentifier).fold(
        e => log.error(e.getMessage, e),
        wikiPages =>
          wikiPages.foreach(page => {
            val detail: WikiPageDetail = redmineService.getWikiPageDetailByProjectAndTitle(project.getIdentifier, page.getTitle)
            users += Option(detail.getUser)
            count += 1
            printlog("-  " + Messages("message.load_redmine_wikis", project.getName, count, wikiPages.size))
          })
      )
      sender ! users.flatten
  }

}

object WikisActor {

  case class Do()

  def actorName = s"WikisActor_$randomUUID"

}
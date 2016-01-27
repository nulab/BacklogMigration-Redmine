package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{AttachmentDownloader, RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.taskadapter.redmineapi.bean.{Project, User, WikiPageDetail}

/**
 * @author uchida
 */
class WikiActor(conf: R2BConfig, project: Project, pageTitle: String) extends Actor with R2BLogging {

  val redmineService: RedmineService = new RedmineService(conf)

  def receive: Receive = {
    case WikiActor.Do =>
      val wiki: WikiPageDetail = redmineService.getWikiPageDetailByProjectAndTitle(project.getIdentifier, pageTitle)
      val users: Seq[User] = redmineService.getUsers()

      IOUtil.output(ConfigBase.Redmine.getWikiPath(project.getIdentifier, pageTitle), RedmineMarshaller.Wiki(wiki, users))
      AttachmentDownloader.wiki(conf.redmineKey, project.getIdentifier,wiki)

      context.stop(self)
  }

}

object WikiActor {

  case class Do()

  def actorName = s"WikiActor_$randomUUID"

}
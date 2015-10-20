package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor._
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.service.{RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.taskadapter.redmineapi.bean.{Attachment, Project, User, WikiPageDetail}

/**
 * @author uchida
 */
class WikiActor(r2bConf: R2BConfig, project: Project, pageTitle: String) extends Actor with R2BLogging {

  val redmineService: RedmineService = new RedmineService(r2bConf)

  def receive: Receive = {
    case WikiActor.Do =>
      val wikiPageDetail: WikiPageDetail = redmineService.getWikiPageDetailByProjectAndTitle(project.getIdentifier, pageTitle)
      val users: Seq[User] = redmineService.getUsers
      val attachments: Array[Attachment] = wikiPageDetail.getAttachments.toArray(new Array[Attachment](wikiPageDetail.getAttachments.size()))

      IOUtil.output(ConfigBase.Redmine.getWikiPath(project.getIdentifier, pageTitle), RedmineMarshaller.Wiki(wikiPageDetail, users))

      attachments.foreach(downloadAttachmentContent)

      context.stop(self)
  }

  private def downloadAttachmentContent(attachment: Attachment) = {
    val dir: String = ConfigBase.Redmine.getWikiAttachmentDir(project.getIdentifier, pageTitle, attachment.getId)
    IOUtil.createDirectory(dir)
    redmineService.downloadAttachmentContent(attachment, dir)
  }
}

object WikiActor {

  case class Do()

  def actorName = s"WikiActor_$randomUUID"

}
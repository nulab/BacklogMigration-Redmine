package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogJsonProtocol, BacklogWiki}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.RedmineUnmarshaller
import com.nulabinc.r2b.service.convert.ConvertWikiPage
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

import scalax.file.Path

/**
  * @author uchida
  */
class WikisActor(pctx: ProjectContext) extends Actor with R2BLogging {

  import BacklogJsonProtocol._

  var wikiSize: Int = 0
  var convertCount: Int = 0

  def receive: Receive = {
    case WikisActor.Do =>
      val paths: Seq[Path] = IOUtil.directoryPaths(ConfigBase.Redmine.getWikisDir(pctx.redmineProjectKey))

      wikiSize = paths.size
      info(Messages("message.execute_wikis_convert", pctx.project.name, wikiSize))
      paths.foreach(convert)
      context.stop(self)
  }

  private def convert(wikiPath: Path) = {
    for {redmineWikiPage <- RedmineUnmarshaller.wiki(wikiPath.path + "/" + ConfigBase.WIKI_FILE_NAME)} yield {

      //Convert and output backlog wiki
      val convertWikiPage = new ConvertWikiPage(pctx)
      val backlogWiki: BacklogWiki = convertWikiPage.execute(redmineWikiPage)
      IOUtil.output(BacklogConfigBase.Backlog.getWikiPath(pctx.backlogProjectKey, redmineWikiPage.title), backlogWiki.toJson.prettyPrint)

      //Copy attachments
      val redmineAttachments: Seq[RedmineAttachment] = redmineWikiPage.attachments
      redmineAttachments.foreach(redmineAttachment => copy(redmineAttachment, redmineWikiPage))

      convertCount += 1
      info(Messages("message.execute_wiki_convert", pctx.project.name, convertCount, wikiSize))

    }
  }

  private def copy(redmineAttachment: RedmineAttachment, redmineWikiPage: RedmineWikiPage) = {
    val dir: String = ConfigBase.Redmine.getWikiAttachmentDir(pctx.redmineProjectKey, redmineWikiPage.title, redmineAttachment.id)
    val redmineFilePath: String = dir + "/" + redmineAttachment.fileName
    val convertFilePath: String = BacklogConfigBase.Backlog.getWikiAttachmentDir(pctx.backlogProjectKey, redmineWikiPage.title, redmineAttachment.id, redmineAttachment.fileName)
    IOUtil.copy(redmineFilePath, convertFilePath)
  }

}

object WikisActor {

  case class Do()

  def actorName = s"WikisActor_$randomUUID"

}
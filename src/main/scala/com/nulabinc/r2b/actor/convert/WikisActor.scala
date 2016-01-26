package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogJsonProtocol, BacklogWiki}
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.{ConvertService, ProjectInfo, RedmineUnmarshaller}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

import scalax.file.Path

/**
 * @author uchida
 */
class WikisActor(r2bConf: R2BConfig, projectInfo: ProjectInfo) extends Actor with R2BLogging {

  import BacklogJsonProtocol._

  var wikiSize: Int = 0
  var convertCount: Int = 0

  def receive: Receive = {
    case WikisActor.Do =>
      val paths: Seq[Path] = IOUtil.directoryPaths(ConfigBase.Redmine.getWikisDir(projectInfo.projectKey.redmine))

      wikiSize = paths.size
      info(Messages("message.execute_wikis_convert", projectInfo.name, wikiSize))
      paths.foreach(convert)
      context.stop(self)
  }

  private def convert(wikiPath: Path) = {
    for {redmineWikiPage <- RedmineUnmarshaller.wiki(wikiPath.path + "/" + ConfigBase.WIKI_FILE_NAME)} yield {
      //Convert and output backlog wiki
      val backlogWiki: BacklogWiki = ConvertService.WikiPage(redmineWikiPage)
      IOUtil.output(BacklogConfigBase.Backlog.getWikiPath(projectInfo.projectKey.backlog, redmineWikiPage.title), backlogWiki.toJson.prettyPrint)

      //Copy attachments
      val redmineAttachments: Seq[RedmineAttachment] = redmineWikiPage.attachments
      redmineAttachments.foreach(redmineAttachment => copy(redmineAttachment, redmineWikiPage))

      convertCount += 1
      info(Messages("message.execute_wiki_convert", projectInfo.name, convertCount, wikiSize))
    }
  }

  private def copy(redmineAttachment: RedmineAttachment, redmineWikiPage: RedmineWikiPage) = {
    val dir: String = ConfigBase.Redmine.getWikiAttachmentDir(projectInfo.projectKey.redmine, redmineWikiPage.title, redmineAttachment.id)
    val redmineFilePath: String = dir + "/" + redmineAttachment.fileName
    val convertFilePath: String = BacklogConfigBase.Backlog.getWikiAttachmentDir(projectInfo.projectKey.backlog, redmineWikiPage.title, redmineAttachment.id, redmineAttachment.fileName)
    IOUtil.copy(redmineFilePath, convertFilePath)
  }

}

object WikisActor {

  case class Do()

  def actorName = s"WikisActor_$randomUUID"

}
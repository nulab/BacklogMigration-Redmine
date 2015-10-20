package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogJsonProtocol, BacklogWiki}
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.{ConvertService, ProjectInfo, RedmineUnmarshaller}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

/**
 * @author uchida
 */
class NewsActor(r2bConf: R2BConfig, projectInfo: ProjectInfo) extends Actor with R2BLogging {

  import BacklogJsonProtocol._

  var newsSize: Int = 0
  var convertCount: Int = 0

  def receive: Receive = {
    case NewsActor.Do =>
      for {redmineNews <- RedmineUnmarshaller.news(projectInfo.projectKey.redmine)} yield {
        newsSize = redmineNews.size
        if (redmineNews.nonEmpty) printlog(Messages("message.execute_news_list_convert", projectInfo.name, newsSize))
        redmineNews.foreach(convert)
      }
      context.stop(self)
  }

  private def convert(redmineNews: RedmineNews) = {
    val redmineWikiPage: RedmineWikiPage = RedmineWikiPage(
      title = redmineNews.title,
      text = Some(redmineNews.description),
      user = redmineNews.user,
      comments = None,
      parentTitle = None,
      createdOn = redmineNews.createdOn,
      updatedOn = redmineNews.createdOn,
      attachments = Seq.empty[RedmineAttachment])
    val backlogWiki: BacklogWiki = ConvertService.WikiPage(redmineWikiPage)
    IOUtil.output(BacklogConfigBase.Backlog.getWikiPath(projectInfo.projectKey.backlog, "news" + redmineNews.id), backlogWiki.toJson.prettyPrint)

    convertCount += 1
    printlog(Messages("message.execute_news_convert", projectInfo.name, convertCount, newsSize))
  }

}

object NewsActor {

  case class Do()

  def actorName = s"NewsActor_$randomUUID"

}

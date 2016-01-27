package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogJsonProtocol, BacklogWiki}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.RedmineUnmarshaller
import com.nulabinc.r2b.service.convert.ConvertWikiPage
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

/**
  * @author uchida
  */
class NewsActor(pctx: ProjectContext) extends Actor with R2BLogging {

  import BacklogJsonProtocol._

  var newsSize: Int = 0
  var convertCount: Int = 0

  def receive: Receive = {
    case NewsActor.Do =>
      for {redmineNews <- RedmineUnmarshaller.news(pctx.redmineProjectKey)} yield {
        newsSize = redmineNews.size
        if (redmineNews.nonEmpty) info(Messages("message.execute_news_list_convert", pctx.project.name, newsSize))
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

    val convertWikiPage = new ConvertWikiPage(pctx)
    val backlogWiki: BacklogWiki = convertWikiPage.execute(redmineWikiPage)
    IOUtil.output(BacklogConfigBase.Backlog.getWikiPath(pctx.backlogProjectKey, "news" + redmineNews.id), backlogWiki.toJson.prettyPrint)

    convertCount += 1
    info(Messages("message.execute_news_convert", pctx.project.name, convertCount, newsSize))

  }

}

object NewsActor {

  case class Do()

  def actorName = s"NewsActor_$randomUUID"

}

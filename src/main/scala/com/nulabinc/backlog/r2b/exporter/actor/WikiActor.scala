package com.nulabinc.backlog.r2b.exporter.actor

import java.net.URL
import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.utils.{IOUtil, Logging}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.nulabinc.backlog.r2b.exporter.service.AttachmentService
import com.taskadapter.redmineapi.bean.WikiPage
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
private[exporter] class WikiActor(exportContext: ExportContext) extends Actor with Logging {

  import com.nulabinc.backlog.migration.common.formatters.BacklogJsonProtocol._
  import WikiActor.ConsoleF

  implicit val wikiWrites = exportContext.wikiWrites

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logger.debug(s"preRestart: reason: $reason, message: $message")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(10.seconds, self, value)
    }
  }

  def receive: Receive = {
    case WikiActor
          .Do(
            wiki: WikiPage,
            completion: CountDownLatch,
            allCount: Int,
            console: ConsoleF
          ) =>
      exportContext.wikiService.optWikiDetail(wiki.getTitle).foreach { wikiDetail =>
        val backlogWiki = Convert.toBacklog(wikiDetail)
        IOUtil.output(
          exportContext.backlogPaths.wikiJson(backlogWiki.name),
          backlogWiki.toJson.prettyPrint
        )

        wikiDetail.getAttachments.asScala.foreach { attachment =>
          val dir = exportContext.backlogPaths
            .wikiAttachmentDirectoryPath(backlogWiki.name)
          val path =
            exportContext.backlogPaths
              .wikiAttachmentPath(backlogWiki.name, attachment.getFileName)

          IOUtil.createDirectory(dir)

          val url: URL = new URL(
            s"${attachment.getContentURL}?key=${exportContext.apiConfig.key}"
          )

          AttachmentService.download(url, path.path.toFile)
        }
      }

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

}

private[exporter] object WikiActor {

  type ConsoleF = (Int, Int) => Unit

  case class Do(
      wiki: WikiPage,
      completion: CountDownLatch,
      allCount: Int,
      console: ConsoleF
  )

}

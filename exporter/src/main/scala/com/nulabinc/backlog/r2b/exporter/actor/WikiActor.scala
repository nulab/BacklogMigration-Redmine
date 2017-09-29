package com.nulabinc.backlog.r2b.exporter.actor

import java.io.{FileOutputStream, InputStream}
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.common.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain.BacklogWiki
import com.nulabinc.backlog.migration.common.utils.{IOUtil, Logging}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.taskadapter.redmineapi.bean.{WikiPage, WikiPageDetail}
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author uchida
  */
private[exporter] class WikiActor(exportContext: ExportContext) extends Actor with Logging {

  implicit val wikiWrites = exportContext.wikiWrites

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    logger.debug(s"preRestart: reason: ${reason}, message: ${message}")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(10.seconds, self, value)
    }
  }

  def receive: Receive = {
    case WikiActor.Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit)) =>
      val wikiDetail: WikiPageDetail = exportContext.wikiService.wikiDetail(wiki.getTitle)

      val backlogWiki = Convert.toBacklog(wikiDetail)
      IOUtil.output(exportContext.backlogPaths.wikiJson(wiki.getTitle), backlogWiki.toJson.prettyPrint)

      wikiDetail.getAttachments.asScala.foreach { attachment =>
        val url: URL = new URL(s"${attachment.getContentURL}?key=${exportContext.apiConfig.key}")
        download(backlogWiki, attachment.getFileName, url.openStream())
      }

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

  private[this] def download(wiki: BacklogWiki, name: String, content: InputStream) = {
    val dir  = exportContext.backlogPaths.wikiAttachmentDirectoryPath(wiki.name)
    val path = exportContext.backlogPaths.wikiAttachmentPath(wiki.name, name)
    IOUtil.createDirectory(dir)

    val rbc = Channels.newChannel(content)
    val fos = new FileOutputStream(path.path)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

    rbc.close()
    fos.close()
  }

}

private[exporter] object WikiActor {

  case class Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit))

}

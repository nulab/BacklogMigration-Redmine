package com.nulabinc.backlog.r2b.mapping.collector.actor

import java.util.concurrent.CountDownLatch

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.mapping.collector.core.MappingData
import com.nulabinc.backlog.r2b.redmine.service.WikiService
import com.taskadapter.redmineapi.bean.{User, WikiPage, WikiPageDetail}
import org.apache.pekko.actor.Actor

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * @author
 *   uchida
 */
private[collector] class WikiActor(
    wikiService: WikiService,
    mappingData: MappingData
) extends Actor
    with Logging {

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    logger.debug(s"preRestart: reason: ${reason}, message: ${message}")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(10.seconds, self, value)
    }
  }

  private[this] val users = mutable.Set.empty[Option[User]]

  def receive: Receive = {
    case WikiActor.Do(
          wiki: WikiPage,
          completion: CountDownLatch,
          allCount: Int,
          console: ((Int, Int) => Unit)
        ) =>
      wikiService.optWikiDetail(wiki.getTitle).foreach { wikiDetail =>
        parse(wikiDetail)
        mappingData.users ++= users.flatten
      }
      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

  private[this] def parse(wikiDetail: WikiPageDetail) =
    users += Option(wikiDetail.getUser)

}

private[collector] object WikiActor {

  case class Do(
      wiki: WikiPage,
      completion: CountDownLatch,
      allCount: Int,
      console: ((Int, Int) => Unit)
  )

}

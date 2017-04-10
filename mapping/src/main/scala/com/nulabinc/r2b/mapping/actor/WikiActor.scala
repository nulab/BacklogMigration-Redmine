package com.nulabinc.r2b.mapping.actor

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.mapping.core.MappingData
import com.nulabinc.r2b.redmine.service.WikiService
import com.taskadapter.redmineapi.bean.{User, WikiPage, WikiPageDetail}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author uchida
  */
class WikiActor(wikiService: WikiService, mappingData: MappingData) extends Actor with Logging {

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    logger.debug(s"preRestart: reason: ${reason}, message: ${message}")
    for { value <- message } yield {
      context.system.scheduler.scheduleOnce(1.minute, self, value)
    }
  }

  private[this] val users = mutable.Set.empty[Option[User]]

  def receive: Receive = {
    case WikiActor.Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit)) =>
      val wikiDetail: WikiPageDetail = wikiService.wikiDetail(wiki.getTitle)
      parse(wikiDetail)
      mappingData.users ++= users.flatten

      completion.countDown()
      console((allCount - completion.getCount).toInt, allCount)
  }

  private[this] def parse(wikiDetail: WikiPageDetail) =
    users += Option(wikiDetail.getUser)

}

object WikiActor {

  case class Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int, console: ((Int, Int) => Unit))

}

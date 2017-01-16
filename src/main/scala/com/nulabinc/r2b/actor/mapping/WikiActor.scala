package com.nulabinc.r2b.actor.mapping

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.WikiService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User, WikiPage, WikiPageDetail}

import scala.collection.mutable

/**
  * @author uchida
  */
class WikiActor(wikiService: WikiService, mappingData: MappingData) extends Actor with Logging {

  private[this] val users = mutable.Set.empty[Option[User]]

  def receive: Receive = {
    case WikiActor.Do(projectKey: String, wiki: WikiPage, completion: CountDownLatch, allCount: Int) =>
      val wikiDetail: WikiPageDetail = wikiService.wikiDetail(wiki.getTitle)
      parse(wikiDetail)
      mappingData.users ++= users.flatten

      completion.countDown()
      log.info(showMessage(LOG_List, Messages("cli.load_redmine_wikis", allCount - completion.getCount, allCount)))
  }

  private[this] def parse(wikiDetail: WikiPageDetail) =
    users += Option(wikiDetail.getUser)

}

object WikiActor {

  case class Do(projectKey: String, wiki: WikiPage, completion: CountDownLatch, allCount: Int)

}

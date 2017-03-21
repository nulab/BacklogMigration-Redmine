package com.nulabinc.r2b.actor.mapping

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.WikiService
import com.taskadapter.redmineapi.bean.WikiPage
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * @author uchida
  */
class WikisActor @Inject()(@Named("projectKey") projectKey: String, wikiService: WikiService) extends Actor with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val wikis: Seq[WikiPage] = wikiService.allWikis()
  private[this] val completion = new CountDownLatch(wikis.size)

  def receive: Receive = {
    case WikisActor.Do(mappingData: MappingData) =>
      val router = SmallestMailboxPool(ConfigFactory.load().getInt("akka.mailbox-pool"), supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(wikiService, mappingData))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(projectKey, wiki, completion, wikis.size))

      completion.await
      context.stop(self)
  }

}

object WikisActor extends NamedActor {

  case class Do(mappingData: MappingData)

  override final val name = "WikisActor"

}
package com.nulabinc.r2b.mapping.actor

import java.util.concurrent.CountDownLatch
import javax.inject.Inject

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.{Logging, ProgressBar}
import com.nulabinc.r2b.mapping.core.MappingData
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.service.WikiService
import com.osinka.i18n.Messages

import scala.concurrent.duration._

/**
  * @author uchida
  */
class WikisActor @Inject()(apiConfig: RedmineConfig, wikiService: WikiService) extends Actor with BacklogConfiguration with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val wikis      = wikiService.allWikis()
  private[this] val completion = new CountDownLatch(wikis.size)
  private[this] val console    = (ProgressBar.progress _)(Messages("common.wikis"), Messages("message.analyzing"), Messages("message.analyzed"))

  def receive: Receive = {
    case WikisActor.Do(mappingData: MappingData) =>
      val router    = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(wikiService, mappingData))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size, console))

      completion.await
      sender() ! WikisActor.Done(mappingData)
  }

}

object WikisActor extends NamedActor {

  override final val name = "WikisActor"

  case class Do(mappingData: MappingData)

  case class Done(mappingData: MappingData)

}

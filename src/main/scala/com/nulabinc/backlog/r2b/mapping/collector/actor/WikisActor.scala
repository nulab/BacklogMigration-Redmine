package com.nulabinc.backlog.r2b.mapping.collector.actor

import java.util.concurrent.CountDownLatch

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.mapping.collector.core.{MappingContext, MappingData}
import com.osinka.i18n.Messages

import scala.concurrent.duration._

/**
  * @author uchida
  */
private[collector] class WikisActor(mappingContext: MappingContext) extends Actor with BacklogConfiguration with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
    case _ => Restart
  }

  private[this] val wikis      = mappingContext.wikiService.allWikis()
  private[this] val completion = new CountDownLatch(wikis.size)
  private[this] val console    = (ProgressBar.progress _)(Messages("common.wikis"), Messages("message.analyzing"), Messages("message.analyzed"))

  def receive: Receive = {
    case WikisActor.Do(mappingData: MappingData) =>
      val router    = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(mappingContext.wikiService, mappingData))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size, console))

      completion.await
      sender() ! WikisActor.Done(mappingData)
  }

}

private[collector] object WikisActor {

  val name = "WikisActor"

  case class Do(mappingData: MappingData)

  case class Done(mappingData: MappingData)

}

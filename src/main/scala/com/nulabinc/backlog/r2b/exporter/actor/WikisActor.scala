package com.nulabinc.backlog.r2b.exporter.actor

import java.util.concurrent.CountDownLatch

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.common.conf.BacklogConfiguration
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.WikiPage

import scala.concurrent.duration._

/**
  * @author uchida
  */
private[exporter] class WikisActor(exportContext: ExportContext) extends Actor with BacklogConfiguration with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
    case _ => Restart
  }

  private[this] val wikis: Seq[WikiPage] = exportContext.wikiService.allWikis()
  private[this] val completion           = new CountDownLatch(wikis.size)
  private[this] val console              = (ProgressBar.progress _)(Messages("common.wikis"), Messages("message.exporting"), Messages("message.exported"))

  def receive: Receive = {
    case WikisActor.Do =>
      val router    = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(exportContext))))
      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size, console))
      completion.await
      sender() ! WikisActor.Done
  }

}

private[exporter] object WikisActor {

  val name = "WikisActor"

  case object Do

  case object Done

}

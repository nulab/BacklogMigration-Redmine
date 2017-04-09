package com.nulabinc.r2b.exporter.actor

import java.util.concurrent.CountDownLatch
import javax.inject.Inject

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.conf.{BacklogConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.{Logging, ProgressBar}
import com.nulabinc.r2b.exporter.convert.{AttachmentWrites, WikiWrites}
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.service.WikiService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.WikiPage

import scala.concurrent.duration._

/**
  * @author uchida
  */
class WikisActor @Inject()(apiConfig: RedmineConfig,
                           backlogPaths: BacklogPaths,
                           wikiWrites: WikiWrites,
                           attachmentWrites: AttachmentWrites,
                           wikiService: WikiService)
    extends Actor
    with BacklogConfiguration
    with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val wikis: Seq[WikiPage] = wikiService.allWikis()
  private[this] val completion           = new CountDownLatch(wikis.size)
  private[this] val console              = (ProgressBar.progress _)(Messages("common.wikis"), Messages("message.exporting"), Messages("message.exported"))

  def receive: Receive = {
    case WikisActor.Do =>
      val router    = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(apiConfig, backlogPaths, wikiWrites, attachmentWrites, wikiService))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size, console))

      completion.await
      sender() ! WikisActor.Done
  }

}

object WikisActor extends NamedActor {

  override final val name = "WikisActor"

  case object Do

  case object Done

}

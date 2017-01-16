package com.nulabinc.r2b.actor.mapping

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, AllForOneStrategy, Props, SupervisorStrategy}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.mapping.MappingData
import com.nulabinc.r2b.service.WikiService
import com.taskadapter.redmineapi.bean.WikiPage
import com.typesafe.config.ConfigFactory


/**
  * @author uchida
  */
class WikisActor @Inject()(@Named("projectKey") projectKey: String, wikiService: WikiService) extends Actor with Logging {

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case _ =>
        Escalate
    }
    AllForOneStrategy()(decider orElse super.supervisorStrategy.decider)
  }

  private[this] val wikis: Seq[WikiPage] = wikiService.allWikis()
  private[this] val completion = new CountDownLatch(wikis.size)

  def receive: Receive = {
    case WikisActor.Do(mappingData: MappingData) =>
      val wikiActor = context.actorOf(SmallestMailboxPool(ConfigFactory.load().getInt("akka.mailbox-pool")).props(Props(new WikiActor(wikiService, mappingData))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(projectKey, wiki, completion, wikis.size))

      completion.await
      context.stop(self)
  }

}

object WikisActor extends NamedActor {

  case class Do(mappingData: MappingData)

  override final val name = "WikisActor"

}
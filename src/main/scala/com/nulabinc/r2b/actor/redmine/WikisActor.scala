package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service.{AttachmentDownloadService, UserService, WikiService}
import com.taskadapter.redmineapi.bean.{User, WikiPage}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
  * @author uchida
  */
class WikisActor @Inject()(
                            redmineDirectory: RedmineDirectory,
                            @Named("key") key: String,
                            attachmentDownloadService: AttachmentDownloadService,
                            userService: UserService,
                            wikiService: WikiService) extends Actor with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val wikis: Seq[WikiPage] = wikiService.allWikis()
  private[this] val completion = new CountDownLatch(wikis.size)

  def receive: Receive = {
    case WikisActor.Do =>
      val users: Seq[User] = userService.allUsers()
      val router = SmallestMailboxPool(ConfigFactory.load().getInt("akka.mailbox-pool"), supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(redmineDirectory, key, attachmentDownloadService, wikiService, users))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size))

      completion.await
      context.stop(self)
  }

}

object WikisActor extends NamedActor {

  case object Do

  override final val name = "WikisActor"

}
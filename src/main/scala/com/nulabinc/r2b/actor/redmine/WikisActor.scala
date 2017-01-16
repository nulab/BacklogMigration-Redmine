package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch
import javax.inject.{Inject, Named}

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, AllForOneStrategy, Props, SupervisorStrategy}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.conf.CommonConfigBase
import com.nulabinc.backlog.migration.di.akkaguice.NamedActor
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service.{AttachmentDownloadService, UserService, WikiService}
import com.taskadapter.redmineapi.bean.{User, WikiPage}

/**
  * @author uchida
  */
class WikisActor @Inject()(
                            redmineDirectory: RedmineDirectory,
                            @Named("key") key: String,
                            @Named("projectKey") projectKey: String,
                            attachmentDownloadService: AttachmentDownloadService,
                            userService: UserService,
                            wikiService: WikiService) extends Actor with Logging {

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
    case WikisActor.Do() =>
      val users: Seq[User] = userService.allUsers()
      val wikiActor = context.actorOf(SmallestMailboxPool(CommonConfigBase.ACTOR_POOL_SIZE).
        props(Props(new WikiActor(redmineDirectory, key, projectKey, attachmentDownloadService, wikiService, users))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size))

      completion.await
      context.stop(self)
  }

}

object WikisActor extends NamedActor {

  case class Do()

  override final val name = "WikisActor"

}
package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service.{AttachmentDownloadService, RedmineMarshaller, WikiService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User, WikiPage, WikiPageDetail}

/**
  * @author uchida
  */
class WikiActor(
                 redmineDirectory: RedmineDirectory,
                 apiKey: String,
                 projectKey: String,
                 attachmentDownloadService: AttachmentDownloadService,
                 wikiService: WikiService,
                 users: Seq[User]) extends Actor with Logging {

  def receive: Receive = {
    case WikiActor.Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int) =>
      val wikiDetail: WikiPageDetail = wikiService.wikiDetail(wiki.getTitle)

      IOUtil.output(redmineDirectory.getWikiPath(wiki.getTitle), RedmineMarshaller.Wiki(wikiDetail, users))
      attachmentDownloadService.wiki(apiKey, projectKey, wikiDetail)

      completion.countDown()
      log.info(showMessage(LOG_List, Messages("cli.load_redmine_wikis", allCount - completion.getCount, allCount)))
  }

}

object WikiActor {

  case class Do(wiki: WikiPage, completion: CountDownLatch, allCount: Int)

}

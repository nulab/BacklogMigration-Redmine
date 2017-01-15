package com.nulabinc.r2b.actor.redmine

import java.util.concurrent.CountDownLatch

import akka.actor.Actor
import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service.{AttachmentDownloadService, IssueService, RedmineMarshaller}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
class IssueActor(
                  redmineDirectory: RedmineDirectory,
                  apiKey: String,
                  projectKey: String,
                  project: Project,
                  attachmentDownloadService: AttachmentDownloadService,
                  issueService: IssueService,
                  users: Seq[User]) extends Actor with Logging {

  def receive: Receive = {
    case IssueActor.Do(issueId: Int, completion: CountDownLatch, allCount: Int) =>
      issueService.issueOfId(issueId, Include.attachments, Include.journals).fold(
        e => throw e,
        issue => {
          IOUtil.output(redmineDirectory.getIssuePath(issueId), RedmineMarshaller.Issue(issue, project, users))
          attachmentDownloadService.issue(apiKey, projectKey, issue)
        }
      )
      completion.countDown()
      log.info(showMessage(LOG_List, Messages("cli.load_redmine_issue", allCount - completion.getCount, allCount)))
  }

}

object IssueActor {

  case class Do(issueId: Int, completion: CountDownLatch, allCount: Int)

}
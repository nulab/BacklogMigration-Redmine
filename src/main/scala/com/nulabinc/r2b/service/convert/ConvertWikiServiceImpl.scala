package com.nulabinc.r2b.service.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.domain.{BacklogAttachment, BacklogSharedFile, BacklogWiki}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.domain.{RedmineAttachment, RedmineWikiPage}
import com.nulabinc.r2b.service.ConvertUserMapping
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class ConvertWikiServiceImpl @Inject()(userMapping: ConvertUserMapping) extends ConvertWikiService with Logging {

  override def convert(redmineWikiPage: RedmineWikiPage): BacklogWiki = {
    val sb = new StringBuilder
    for {text <- redmineWikiPage.text} yield {
      sb.append(text)
    }
    for {comments <- redmineWikiPage.comments} yield {
      sb.append("\n\n\n").append(Messages("common.comment")).append(":").append(comments)
    }
    for {parentTitle <- redmineWikiPage.parentTitle} yield {
      sb.append("\n").append(Messages("common.parent_page")).append(":[[").append(parentTitle).append("]]")
    }
    val content: Option[String] = if (sb.result().isEmpty) None else Some(sb.result())
    BacklogWiki(
      name = redmineWikiPage.title,
      content = content,
      attachments = getBacklogAttachments(redmineWikiPage.attachments),
      sharedFiles = Seq.empty[BacklogSharedFile],
      createdUserId = redmineWikiPage.user.map(userMapping.convert),
      created = redmineWikiPage.createdOn,
      updatedUserId = redmineWikiPage.user.map(userMapping.convert),
      updated = redmineWikiPage.updatedOn)
  }

  private[this] def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
    attachments.map(getBacklogAttachment)

  private[this] def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
    BacklogAttachment(attachment.id, attachment.fileName)

}
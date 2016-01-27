package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.domain.{BacklogAttachment, BacklogWiki}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.domain.{RedmineAttachment, RedmineWikiPage}
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class ConvertWikiPage(pctx: ProjectContext) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def execute(redmineWikiPage: RedmineWikiPage): BacklogWiki = {
    val sb = new StringBuilder
    if (redmineWikiPage.text.isDefined) sb.append(redmineWikiPage.text.get)
    if (redmineWikiPage.comments.isDefined) sb.append("\n\n\n").append(Messages("label.comment")).append(":").append(redmineWikiPage.comments.get)
    if (redmineWikiPage.parentTitle.isDefined) sb.append("\n").append(Messages("label.parent_page")).append(":[[").append(redmineWikiPage.parentTitle.get).append("]]")
    val content: Option[String] = if (sb.result().isEmpty) None else Some(sb.result())

    BacklogWiki(
      name = redmineWikiPage.title,
      content = content,
      createdUserId = redmineWikiPage.user.map(pctx.userMapping.convert),
      created = redmineWikiPage.createdOn,
      updatedUserId = redmineWikiPage.user.map(pctx.userMapping.convert),
      updated = redmineWikiPage.updatedOn,
      attachments = getBacklogAttachments(redmineWikiPage.attachments))
  }

  private def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
    attachments.map(getBacklogAttachment)

  private def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
    BacklogAttachment(attachment.id, attachment.fileName)

}
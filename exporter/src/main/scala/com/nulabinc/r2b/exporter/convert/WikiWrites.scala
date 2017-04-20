package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.{Convert, Writes}
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.{DateUtil, Logging}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.WikiPageDetail

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class WikiWrites @Inject()(implicit val attachmentWrites: AttachmentWrites, implicit val userWrites: UserWrites)
    extends Writes[WikiPageDetail, BacklogWiki]
    with Logging {

  override def writes(wiki: WikiPageDetail): BacklogWiki = {
    BacklogWiki(
      optId = None,
      name = convert(wiki.getTitle),
      optContent = Some(content(wiki)),
      attachments = wiki.getAttachments.asScala.map(Convert.toBacklog(_)),
      sharedFiles = Seq.empty[BacklogSharedFile],
      optCreatedUser = Option(wiki.getUser).map(Convert.toBacklog(_)),
      optCreated = Option(wiki.getCreatedOn).map(DateUtil.isoFormat),
      optUpdatedUser = Option(wiki.getUser).map(Convert.toBacklog(_)),
      optUpdated = Option(wiki.getCreatedOn).map(DateUtil.isoFormat)
    )
  }

  private[this] def convert(title: String) = {
    if (title == "Wiki") "Home" else title
  }

  private[this] def content(wiki: WikiPageDetail): String = {
    val sb = new StringBuilder
    sb.append(wiki.getText)
    for { comments <- Option(wiki.getComments) } yield {
      sb.append("\n\n\n")
      sb.append(Messages("common.comment")).append(":").append(comments)
    }
    for { parent <- Option(wiki.getParent) } yield {
      sb.append("\n")
      sb.append(Messages("common.parent_page")).append(":[[").append(parent.getTitle).append("]]")
    }
    sb.toString()
  }

}

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
    val sb = new StringBuilder
    sb.append(wiki.getText)
    sb.append("\n\n\n").append(Messages("common.comment")).append(":").append(wiki.getComments)
    BacklogWiki(
      optId = None,
      name = wiki.getTitle,
      optContent = Some(sb.toString()),
      attachments = wiki.getAttachments.asScala.map(Convert.toBacklog(_)),
      sharedFiles = Seq.empty[BacklogSharedFile],
      optCreatedUser = Option(wiki.getUser).map(Convert.toBacklog(_)),
      optCreated = Option(wiki.getCreatedOn).map(DateUtil.isoFormat),
      optUpdatedUser = Option(wiki.getUser).map(Convert.toBacklog(_)),
      optUpdated = Option(wiki.getCreatedOn).map(DateUtil.isoFormat)
    )
  }

}

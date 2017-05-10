package com.nulabinc.r2b.exporter.service

import com.nulabinc.backlog.migration.common.utils.FileUtil
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.taskadapter.redmineapi.bean.{Attachment, Journal, JournalDetail}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class AttachmentFilter(journals: Seq[Journal]) {

  def filter(attachments: Seq[Attachment]): Seq[Attachment] = {
    attachments.filterNot(condition)
  }

  private[this] def condition(attachment: Attachment): Boolean =
    journals.exists(journal => condition(journal, attachment))

  private[this] def condition(journal: Journal, attachment: Attachment): Boolean =
    journal.getDetails.asScala.exists(detail => condition(detail, attachment))

  private[this] def condition(detail: JournalDetail, attachment: Attachment): Boolean = {
    detail.getProperty match {
      case RedmineConstantValue.ATTACHMENT => FileUtil.normalize(attachment.getFileName) == FileUtil.normalize(detail.getNewValue)
      case _                               => false
    }
  }

}

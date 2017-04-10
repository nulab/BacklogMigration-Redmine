package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.nulabinc.backlog.migration.utils.FileUtil
import com.taskadapter.redmineapi.bean.Attachment

/**
  * @author uchida
  */
class AttachmentWrites @Inject()() extends Writes[Attachment, BacklogAttachment] {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      id = attachment.getId.intValue(),
      fileName = FileUtil.clean(attachment.getFileName)
    )
  }

}

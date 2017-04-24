package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogAttachment
import com.taskadapter.redmineapi.bean.Attachment

/**
  * @author uchida
  */
class AttachmentWrites @Inject()() extends Writes[Attachment, BacklogAttachment] {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      optId = Some(attachment.getId.intValue()),
      name = attachment.getFileName
    )
  }

}

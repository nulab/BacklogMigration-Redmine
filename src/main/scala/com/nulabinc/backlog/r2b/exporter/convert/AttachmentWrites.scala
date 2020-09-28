package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogAttachment
import com.nulabinc.backlog.migration.common.utils.FileUtil
import com.taskadapter.redmineapi.bean.Attachment

/**
  * @author uchida
  */
private[exporter] class AttachmentWrites @Inject() ()
    extends Writes[Attachment, BacklogAttachment] {

  override def writes(attachment: Attachment): BacklogAttachment = {
    BacklogAttachment(
      optId = Some(attachment.getId.intValue()),
      name = FileUtil.normalize(attachment.getFileName)
    )
  }

}

package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.{BacklogAttachmentInfo, BacklogAttributeInfo, BacklogChangeLog}
import com.nulabinc.backlog.migration.utils.{DateUtil, FileUtil}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.domain.CustomFieldFormats
import com.taskadapter.redmineapi.bean.JournalDetail

/**
  * @author uchida
  */
class JournalDetailWrites @Inject()(customFieldFormats: CustomFieldFormats) extends Writes[JournalDetail, BacklogChangeLog] {

  override def writes(detail: JournalDetail): BacklogChangeLog = {
    BacklogChangeLog(
      field = field(detail),
      optOriginalValue = Option(detail.getOldValue).map(DateUtil.formatIfNeeded),
      optNewValue = Option(detail.getNewValue).map(DateUtil.formatIfNeeded),
      optAttachmentInfo = attachmentInfo(detail),
      optAttributeInfo = attributeInfo(detail),
      optNotificationInfo = None
    )
  }

  private[this] def attributeInfo(detail: JournalDetail): Option[BacklogAttributeInfo] = {
    val optTypeId: Option[Int] = customFieldFormats.map.get(detail.getName) match {
      case Some(definition) =>
        definition.fieldFormat match {
          case RedmineConstantValue.FieldFormat.TEXT                                           => Some(FieldType.Text.getIntValue)
          case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK => Some(FieldType.TextArea.getIntValue)
          case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT   => Some(FieldType.Numeric.getIntValue)
          case RedmineConstantValue.FieldFormat.DATE                                           => Some(FieldType.Date.getIntValue)
          case RedmineConstantValue.FieldFormat.BOOL                                           => Some(FieldType.SingleList.getIntValue)
          case RedmineConstantValue.FieldFormat.LIST if (!definition.multiple)                 => Some(FieldType.SingleList.getIntValue)
          case RedmineConstantValue.FieldFormat.LIST if (definition.multiple)                  => Some(FieldType.MultipleList.getIntValue)
          case RedmineConstantValue.FieldFormat.VERSION                                        => Some(FieldType.MultipleList.getIntValue)
          case RedmineConstantValue.FieldFormat.USER                                           => Some(FieldType.MultipleList.getIntValue)
          case _                                                                               => None
        }
      case _ => None
    }
    optTypeId.map(typeId => BacklogAttributeInfo(optId = None, typeId = typeId.toString))
  }

  private[this] def attachmentInfo(detail: JournalDetail): Option[BacklogAttachmentInfo] = {
    detail.getProperty match {
      case RedmineConstantValue.ATTACHMENT =>
        val attachment = BacklogAttachmentInfo(optId = Some(detail.getName.toInt), name = FileUtil.clean(detail.getNewValue))
        Some(attachment)
      case _ => None
    }
  }

  private[this] def field(detail: JournalDetail): String = detail.getProperty match {
    case RedmineConstantValue.CUSTOM_FIELD => detail.getName
    case RedmineConstantValue.ATTACHMENT   => BacklogConstantValue.ChangeLog.ATTACHMENT
    case _                                 => field(detail.getName)
  }

  private def field(name: String): String = name match {
    case RedmineConstantValue.Attr.SUBJECT     => BacklogConstantValue.ChangeLog.SUMMARY
    case RedmineConstantValue.Attr.DESCRIPTION => BacklogConstantValue.ChangeLog.DESCRIPTION
    case RedmineConstantValue.Attr.CATEGORY    => BacklogConstantValue.ChangeLog.COMPONENT
    //version
    case RedmineConstantValue.Attr.VERSION    => BacklogConstantValue.ChangeLog.MILESTONE
    case RedmineConstantValue.Attr.STATUS     => BacklogConstantValue.ChangeLog.STATUS
    case RedmineConstantValue.Attr.ASSIGNED   => BacklogConstantValue.ChangeLog.ASSIGNER
    case RedmineConstantValue.Attr.TRACKER    => BacklogConstantValue.ChangeLog.ISSUE_TYPE
    case RedmineConstantValue.Attr.START_DATE => BacklogConstantValue.ChangeLog.START_DATE
    case RedmineConstantValue.Attr.DUE_DATE   => BacklogConstantValue.ChangeLog.LIMIT_DATE
    case RedmineConstantValue.Attr.PRIORITY   => BacklogConstantValue.ChangeLog.PRIORITY
    //resolution
    case RedmineConstantValue.Attr.ESTIMATED_HOURS => BacklogConstantValue.ChangeLog.ESTIMATED_HOURS
    //actualHours
    case RedmineConstantValue.Attr.PARENT => BacklogConstantValue.ChangeLog.PARENT_ISSUE
    //notification
    //attachment
    //commit
    case _ => name
  }

}

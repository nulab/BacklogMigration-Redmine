package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{BacklogAttachment, BacklogAttributeInfo, BacklogChangeLog, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, FileUtil, Logging, StringUtil}
import com.nulabinc.backlog.r2b.mapping.service.{MappingPriorityService, MappingStatusService, MappingUserService}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue
import com.nulabinc.backlog.r2b.utils.TextileUtil
import com.nulabinc.backlog4j.CustomField.FieldType
import com.taskadapter.redmineapi.bean.JournalDetail

/**
  * @author uchida
  */
private[exporter] class JournalDetailWrites @Inject()(propertyValue: PropertyValue,
                                                      customFieldValueWrites: CustomFieldValueWrites,
                                                      mappingPriorityService: MappingPriorityService,
                                                      mappingStatusService: MappingStatusService,
                                                      mappingUserService: MappingUserService,
                                                      backlogTextFormattingRule: BacklogTextFormattingRule)
    extends Writes[JournalDetail, BacklogChangeLog]
    with Logging {

  override def writes(detail: JournalDetail): BacklogChangeLog = {
    BacklogChangeLog(
      field = field(detail),
      optOriginalValue = Option(detail.getOldValue).flatMap(value => detailValue(detail, value)).map(DateUtil.formatIfNeeded),
      optNewValue = Option(detail.getNewValue).flatMap(value => detailValue(detail, value)).map(DateUtil.formatIfNeeded),
      optAttachmentInfo = attachmentInfo(detail),
      optAttributeInfo = attributeInfo(detail),
      optNotificationInfo = None
    )
  }

  private[this] def attributeInfo(detail: JournalDetail): Option[BacklogAttributeInfo] = {
    detail.getProperty match {
      case RedmineConstantValue.CUSTOM_FIELD =>
        val optCustomFieldDefinition = propertyValue.customFieldDefinitionOfId(detail.getName)
        val optTypeId = optCustomFieldDefinition match {
          case Some(customFieldDefinition) =>
            customFieldDefinition.fieldFormat match {
              case RedmineConstantValue.FieldFormat.TEXT                                           => Some(FieldType.Text.getIntValue)
              case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK => Some(FieldType.TextArea.getIntValue)
              case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT   => Some(FieldType.Numeric.getIntValue)
              case RedmineConstantValue.FieldFormat.DATE                                           => Some(FieldType.Date.getIntValue)
              case RedmineConstantValue.FieldFormat.BOOL                                           => Some(FieldType.SingleList.getIntValue)
              case RedmineConstantValue.FieldFormat.LIST if (!customFieldDefinition.isMultiple)    => Some(FieldType.SingleList.getIntValue)
              case RedmineConstantValue.FieldFormat.LIST if (customFieldDefinition.isMultiple)     => Some(FieldType.MultipleList.getIntValue)
              case RedmineConstantValue.FieldFormat.VERSION                                        => Some(FieldType.MultipleList.getIntValue)
              case RedmineConstantValue.FieldFormat.USER                                           => Some(FieldType.MultipleList.getIntValue)
              case _                                                                               => None
            }
          case _ => throw new RuntimeException(s"custom field id not found [${detail.getName}]")
        }
        optTypeId.map(typeId => BacklogAttributeInfo(optId = None, typeId = typeId.toString))
      case _ => None
    }
  }

  private[this] def attachmentInfo(detail: JournalDetail): Option[BacklogAttachment] = {
    detail.getProperty match {
      case RedmineConstantValue.ATTACHMENT =>
        val attachment = BacklogAttachment(optId = StringUtil.safeStringToLong(detail.getName), name = FileUtil.normalize(detail.getNewValue))
        Some(attachment)
      case _ => None
    }
  }

  private[this] def detailValue(detail: JournalDetail, value: String): Option[String] =
    detail.getProperty match {
      case RedmineConstantValue.ATTR         => attr(detail, value)
      case RedmineConstantValue.CUSTOM_FIELD => Convert.toBacklog((detail.getName, Option(value)))(customFieldValueWrites)
      case RedmineConstantValue.ATTACHMENT   => Option(value)
      case RedmineConstantValue.RELATION     => Option(value)
    }

  private[this] def attr(detail: JournalDetail, value: String): Option[String] =
    detail.getName match {
      case RedmineConstantValue.Attr.STATUS =>
        propertyValue.statuses.find(status => StringUtil.safeEquals(status.getId.intValue(), value)).map(_.getName).map(mappingStatusService.convert)
      case RedmineConstantValue.Attr.PRIORITY =>
        propertyValue.priorities
          .find(priority => StringUtil.safeEquals(priority.getId.intValue(), value))
          .map(_.getName)
          .map(mappingPriorityService.convert)
      case RedmineConstantValue.Attr.ASSIGNED =>
        propertyValue.optUserOfId(Some(value)).map(_.getLogin).map(mappingUserService.convert)
      case RedmineConstantValue.Attr.VERSION =>
        propertyValue.versions.find(version => StringUtil.safeEquals(version.getId.intValue(), value)).map(_.getName)
      case RedmineConstantValue.Attr.TRACKER =>
        propertyValue.trackers.find(tracker => StringUtil.safeEquals(tracker.getId.intValue(), value)).map(_.getName)
      case RedmineConstantValue.Attr.CATEGORY =>
        propertyValue.categories.find(category => StringUtil.safeEquals(category.getId.intValue(), value)).map(_.getName)
      case _ => Option(TextileUtil.convert(value, backlogTextFormattingRule))
    }

  private[this] def field(detail: JournalDetail): String = detail.getProperty match {
    case RedmineConstantValue.CUSTOM_FIELD =>
      propertyValue
        .customFieldDefinitionOfId(detail.getName)
        .map(_.name)
        .getOrElse {
          val message = propertyValue.customFieldDefinitions.map(c => s"${c.id}: ${c.name}").mkString("\n")
          throw new RuntimeException(s"custom field id not found. Custom field name: ${detail.getName}\nAvailable custom fields are:\n$message")
        }
    case RedmineConstantValue.ATTACHMENT =>
      BacklogConstantValue.ChangeLog.ATTACHMENT
    case _ =>
      field(detail.getName)
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

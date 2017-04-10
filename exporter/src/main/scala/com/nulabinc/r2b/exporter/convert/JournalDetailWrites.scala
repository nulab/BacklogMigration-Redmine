package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.{BacklogAttachmentInfo, BacklogAttributeInfo, BacklogChangeLog}
import com.nulabinc.backlog.migration.utils.DateUtil
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.r2b.mapping.core.{ConvertPriorityMapping, ConvertStatusMapping, ConvertUserMapping}
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.domain.CustomFieldFormats
import com.nulabinc.r2b.redmine.service._
import com.taskadapter.redmineapi.bean.JournalDetail

/**
  * @author uchida
  */
class JournalDetailWrites @Inject()(customFieldFormats: CustomFieldFormats,
                                    statusService: StatusService,
                                    priorityService: PriorityService,
                                    userService: UserService,
                                    versionService: VersionService,
                                    trackerService: TrackerService,
                                    categoryService: IssueCategoryService)
    extends Writes[JournalDetail, BacklogChangeLog] {

  val userMapping     = new ConvertUserMapping()
  val statusMapping   = new ConvertStatusMapping()
  val priorityMapping = new ConvertPriorityMapping()

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
        val attachment = BacklogAttachmentInfo(optId = Some(detail.getName.toInt), name = detail.getNewValue)
        Some(attachment)
      case _ => None
    }
  }

  private[this] def detailValue(detail: JournalDetail, value: String): Option[String] =
    detail.getProperty match {
      case RedmineConstantValue.ATTR         => attr(detail, value)
      case RedmineConstantValue.CUSTOM_FIELD => cf(detail, value)
      case RedmineConstantValue.ATTACHMENT   => Option(value)
      case RedmineConstantValue.RELATION     => Option(value)
    }

  private[this] def cf(detail: JournalDetail, value: String): Option[String] = {
    customFieldFormats.map.get(detail.getName) match {
      case Some(definition) =>
        definition.fieldFormat match {
          case RedmineConstantValue.FieldFormat.VERSION =>
            versionService.allVersions().find(version => version.getId == value.toInt).map(_.getName)
          case RedmineConstantValue.FieldFormat.USER =>
            userService.optUserOfId(value.toInt).map(_.getLogin).map(userMapping.convert)
          case _ => Option(value)
        }
      case _ => Option(value)
    }
  }

  private[this] def attr(detail: JournalDetail, value: String): Option[String] =
    detail.getName match {
      case RedmineConstantValue.Attr.STATUS =>
        statusService.allStatuses().find(status => status.getId == value.toInt).map(_.getName).map(statusMapping.convert)
      case RedmineConstantValue.Attr.PRIORITY =>
        priorityService.allPriorities().find(priority => priority.getId == value.toInt).map(_.getName).map(priorityMapping.convert)
      case RedmineConstantValue.Attr.ASSIGNED =>
        userService.optUserOfId(value.toInt).map(_.getLogin).map(userMapping.convert)
      case RedmineConstantValue.Attr.VERSION =>
        versionService.allVersions().find(version => version.getId == value.toInt).map(_.getName)
      case RedmineConstantValue.Attr.TRACKER =>
        trackerService.allTrackers().find(tracker => tracker.getId == value.toInt).map(_.getName)
      case RedmineConstantValue.Attr.CATEGORY =>
        categoryService.allCategories().find(category => category.getId == value.toInt).map(_.getName)
      case _ => Option(value)
    }

  private[this] def field(detail: JournalDetail): String = detail.getProperty match {
    case RedmineConstantValue.CUSTOM_FIELD =>
      val optDefinition = customFieldFormats.map.find {
        case (_, definition) =>
          definition.id == detail.getName.toInt
      }
      optDefinition match {
        case Some((name, _)) => name
        case _               => throw new RuntimeException(s"custom field id not found [${detail.getName}]")
      }
    case RedmineConstantValue.ATTACHMENT => BacklogConstantValue.ChangeLog.ATTACHMENT
    case _                               => field(detail.getName)
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

package com.nulabinc.r2b.service.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogProperty
import com.nulabinc.backlog.migration.domain.{BacklogChangeLog, BacklogComment}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineProperty
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineJournal, RedmineJournalDetail}
import com.nulabinc.r2b.service._
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class ConvertCommentServiceImpl @Inject()(
                                           propertyService: PropertyService,
                                           customFieldService: CustomFieldService,
                                           userMapping: ConvertUserMapping,
                                           convertJournalDetailService: ConvertJournalDetailService) extends ConvertCommentService with Logging {

  private[this] val customFieldDefinitions = customFieldService.allCustomFieldDefinitions()

  override def convert(journals: Seq[RedmineJournal], issueId: Int): Seq[BacklogComment] =
    journals.map(journal => getBacklogComment(journal, issueId))

  private[this] def getBacklogComment(journal: RedmineJournal, issueId: Int): BacklogComment = {
    val details = journal.details.filterNot(detail => convertJournalDetailService.needNote(detail, issueId))
    val notes = journal.details.filter(detail => convertJournalDetailService.needNote(detail, issueId))
    val convertOldValue = convertOldValueFunc()
    BacklogComment(
      content = journal.notes.getOrElse("") + "\n" + getNoteText(notes, issueId),
      changeLogs = details.map(detail => getBacklogChangeLog(detail, convertOldValue)),
      notificationUserIds = Seq.empty[String],
      isCreateIssue = false,
      createdUserId = journal.user.map(userMapping.convert),
      created = journal.createdOn)
  }

  private[this] def getBacklogChangeLog(detail: RedmineJournalDetail, convertOldValue: (RedmineJournalDetail, Option[String]) => Option[String]): BacklogChangeLog = {
    val convertOld = convertOldValue(detail, detail.oldValue)
    val convertNew = convertValue(detail, detail.newValue)
    BacklogChangeLog(
      property = detail.property,
      name = convertName(detail),
      oldValue = convertOld,
      newValue = convertNew)
  }

  private[this] def convertOldValueFunc() = {
    var isSetStatus: Boolean = false
    (detail: RedmineJournalDetail, value: Option[String]) => {
      val optValue: Option[String] = convertValue(detail, detail.oldValue)
      if (detail.property == RedmineProperty.ATTR && detail.name == RedmineProperty.Attr.STATUS) {
        if (isSetStatus) optValue
        else {
          isSetStatus = true
          propertyService.optDefaultStatusName()
        }
      } else optValue
    }: Option[String]
  }

  private[this] def getNoteText(details: Seq[RedmineJournalDetail], issueId: Int): String =
    details.map(detail => convertJournalDetailService.getValue(detail, issueId)).mkString("\n")

  private[this] def convertName(detail: RedmineJournalDetail): String = detail.property match {
    case RedmineProperty.CUSTOM_FIELD => customFieldDefinitionNameOfId(detail.name)
    case _ => convertBacklogName(detail.name)
  }

  private[this] def convertBacklogName(name: String): String = name match {
    case RedmineProperty.Attr.SUBJECT => BacklogProperty.Attr.SUMMARY
    case RedmineProperty.Attr.DESCRIPTION => BacklogProperty.Attr.DESCRIPTION
    case RedmineProperty.Attr.TRACKER => BacklogProperty.Attr.ISSUE_TYPE
    case RedmineProperty.Attr.STATUS => BacklogProperty.Attr.STATUS
    case RedmineProperty.Attr.PRIORITY => BacklogProperty.Attr.PRIORITY
    case RedmineProperty.Attr.ASSIGNED => BacklogProperty.Attr.ASSIGNEE
    case RedmineProperty.Attr.VERSION => BacklogProperty.Attr.MILESTONE
    case RedmineProperty.Attr.PARENT => BacklogProperty.Attr.PARENT_ISSUE
    case RedmineProperty.Attr.START_DATE => BacklogProperty.Attr.START_DATE
    case RedmineProperty.Attr.DUE_DATE => BacklogProperty.Attr.DUE_DATE
    case RedmineProperty.Attr.ESTIMATED_HOURS => BacklogProperty.Attr.ESTIMATED_HOURS
    case RedmineProperty.Attr.CATEGORY => BacklogProperty.Attr.CATEGORY
    case _ => name
  }

  private[this] def convertValue(detail: RedmineJournalDetail, value: Option[String]): Option[String] = detail.property match {
    case RedmineProperty.ATTR => convertAttr(detail.name, value)
    case RedmineProperty.CUSTOM_FIELD => convertCf(detail.name, value)
    case RedmineProperty.ATTACHMENT => value
    case RedmineProperty.RELATION => value
  }

  private[this] def convertAttr(name: String, value: Option[String]): Option[String] = name match {
    case RedmineProperty.Attr.STATUS => propertyService.optStatusName(value)
    case RedmineProperty.Attr.PRIORITY => propertyService.optPriorityName(value)
    case RedmineProperty.Attr.ASSIGNED => propertyService.optUser(value).map(userMapping.convert)
    case RedmineProperty.Attr.VERSION => propertyService.optVersionName(value)
    case RedmineProperty.Attr.TRACKER => propertyService.optTrackerName(value)
    case RedmineProperty.Attr.CATEGORY => propertyService.optCategoryName(value)
    case _ => value
  }

  private[this] def convertCf(name: String, value: Option[String]): Option[String] =
    optCustomFieldDefinition(name.toInt) match {
      case Some(customFieldDefinition) =>
        customFieldDefinition.fieldFormat match {
          case "version" => propertyService.optVersionName(value)
          case "user" => propertyService.optUserName(value)
          case "bool" =>
            value match {
              case Some("0") => Some(Messages("common.no"))
              case Some("1") => Some(Messages("common.yes"))
              case _ => None
            }
          case _ => value
        }
      case _ => value
    }

  private[this] def optCustomFieldDefinition(id: Int): Option[RedmineCustomFieldDefinition] =
    customFieldDefinitions.find(_.id == id)

  private[this] def customFieldDefinitionNameOfId(id: String): String =
    customFieldDefinitions.find(_.id == id.toInt).map(_.name).getOrElse("")

}

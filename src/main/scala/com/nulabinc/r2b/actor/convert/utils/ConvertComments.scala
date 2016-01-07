package com.nulabinc.r2b.actor.convert.utils

import java.util.Locale

import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogComment, BacklogCommentDetail}
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineJournal, RedmineJournalDetail}
import com.nulabinc.r2b.service.{ConvertUserMapping, ProjectEnumerations, RedmineUnmarshaller}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
object ConvertComments {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def apply(projectIdentifier: String, issueId: Int, projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping, journals: Seq[RedmineJournal]): Seq[BacklogComment] =
    journalsToComments(projectIdentifier, issueId, projectEnumerations, userMapping)(journals)

  private def journalsToComments(projectIdentifier: String, issueId: Int, projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping)(journals: Seq[RedmineJournal]) =
    journals.map(journalToComment(projectIdentifier, issueId, projectEnumerations, userMapping)(_))

  private def journalToComment(projectIdentifier: String, issueId: Int, projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping)(journal: RedmineJournal) =
    getBacklogComment(projectIdentifier, issueId, projectEnumerations, userMapping, journal)

  private def getBacklogComment(projectIdentifier: String, issueId: Int, projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping, redmineJournal: RedmineJournal): BacklogComment =
    BacklogComment(
      content = redmineJournal.notes + "\n" + getOtherProperty(projectIdentifier, issueId, redmineJournal.details),
      details = journalDetailsToCommentDetails(projectEnumerations, userMapping)(redmineJournal.details.filter(detail => !isOtherProperty(projectIdentifier, issueId, detail))),
      createdUserId = redmineJournal.user.map(userMapping.convert),
      created = redmineJournal.createdOn)

  private def journalDetailsToCommentDetails(projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping)(details: Seq[RedmineJournalDetail]) =
    details.map(journalDetailToCommentDetail(projectEnumerations, userMapping)(_))

  private def journalDetailToCommentDetail(projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping)(detail: RedmineJournalDetail) =
    getBacklogCommentDetail(projectEnumerations, userMapping, detail)

  private def getBacklogCommentDetail(projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping, redmineJournalDetail: RedmineJournalDetail): BacklogCommentDetail =
    BacklogCommentDetail(
      property = redmineJournalDetail.property,
      name = convertName(projectEnumerations, redmineJournalDetail.property, redmineJournalDetail.name),
      oldValue = convertValue(projectEnumerations, userMapping, redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.oldValue),
      newValue = convertValue(projectEnumerations, userMapping, redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.newValue))

  private def getOtherProperty(projectIdentifier: String, issueId: Int, details: Seq[RedmineJournalDetail]): String =
    details.filter(isOtherProperty(projectIdentifier, issueId, _)).map(getOtherPropertyMessage(projectIdentifier, issueId, _)).mkString("\n")

  private def getOtherPropertyMessage(projectIdentifier: String, issueId: Int, detail: RedmineJournalDetail): String =
    if (isDoneRatioJournal(detail)) createMessage("label.done_ratio", detail)
    else if (isPrivateJournal(detail)) createMessage("label.private", detail)
    else if (isProjectId(detail)) createMessage("label.project", detail)
    else if (isRelationJournal(detail)) createMessage("label.relation", detail)
    else if (isAttachmentNotFound(projectIdentifier, issueId, detail: RedmineJournalDetail)) {
      if (detail.newValue.isDefined) Messages("message.add_attachment", detail.newValue.get)
      else if (detail.oldValue.isDefined) Messages("message.del_attachment", detail.oldValue.get)
      else ""
    }
    else ""

  private def createMessage(label: String, detail: RedmineJournalDetail): String =
    Messages("label.change_comment", Messages(label), getStringMessage(detail.oldValue), getStringMessage(detail.newValue))

  private def isOtherProperty(projectIdentifier: String, issueId: Int, detail: RedmineJournalDetail): Boolean =
    isRelationJournal(detail) || isDoneRatioJournal(detail) || isPrivateJournal(detail) || isProjectId(detail) ||
      isAttachmentNotFound(projectIdentifier, issueId, detail: RedmineJournalDetail)

  private def isAttachmentNotFound(projectIdentifier: String, issueId: Int, detail: RedmineJournalDetail): Boolean = {
    if (detail.property == ConfigBase.Property.ATTACHMENT) {
      val path: String = ConfigBase.Redmine.getIssueAttachmentDir(projectIdentifier, issueId, detail.name.toInt)
      !IOUtil.isDirectory(path)
    } else false
  }

  private def isRelationJournal(detail: RedmineJournalDetail) = detail.property == "relation"

  private def isDoneRatioJournal(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "done_ratio"

  private def isPrivateJournal(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "is_private"

  private def isProjectId(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "project_id"

  private def getStringMessage(value: Option[String]): String =
    if (value.isEmpty) Messages("label.not_set")
    else value.get

  private def convertName(projectEnumerations: ProjectEnumerations, property: String, name: String): String = property match {
    case ConfigBase.Property.CF => projectEnumerations.CustomFieldDefinitions.convertValue(Some(name)).get
    case _ => convertBacklogName(name)
  }

  private def convertBacklogName(name: String): String = name match {
    case ConfigBase.Property.Attr.SUBJECT => BacklogConfigBase.Property.Attr.SUMMARY
    case ConfigBase.Property.Attr.TRACKER => BacklogConfigBase.Property.Attr.ISSUE_TYPE
    case ConfigBase.Property.Attr.STATUS => BacklogConfigBase.Property.Attr.STATUS
    case ConfigBase.Property.Attr.PRIORITY => BacklogConfigBase.Property.Attr.PRIORITY
    case ConfigBase.Property.Attr.ASSIGNED => BacklogConfigBase.Property.Attr.ASSIGNED
    case ConfigBase.Property.Attr.VERSION => BacklogConfigBase.Property.Attr.VERSION
    case ConfigBase.Property.Attr.PARENT => BacklogConfigBase.Property.Attr.PARENT
    case ConfigBase.Property.Attr.START_DATE => BacklogConfigBase.Property.Attr.START_DATE
    case ConfigBase.Property.Attr.DUE_DATE => BacklogConfigBase.Property.Attr.DUE_DATE
    case ConfigBase.Property.Attr.ESTIMATED_HOURS => BacklogConfigBase.Property.Attr.ESTIMATED_HOURS
    case ConfigBase.Property.Attr.CATEGORY => BacklogConfigBase.Property.Attr.CATEGORY
    case _ => name
  }

  private def convertValue(projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping, property: String, name: String, value: Option[String]): Option[String] = property match {
    case ConfigBase.Property.ATTR => convertAttr(projectEnumerations, userMapping, name, value)
    case ConfigBase.Property.CF => convertCf(projectEnumerations, name, value)
    case ConfigBase.Property.ATTACHMENT => value
    case "relation" => value
  }

  private def convertAttr(projectEnumerations: ProjectEnumerations, userMapping: ConvertUserMapping, name: String, value: Option[String]): Option[String] = name match {
    case ConfigBase.Property.Attr.STATUS => projectEnumerations.IssueStatus.convertValue(value)
    case ConfigBase.Property.Attr.PRIORITY => projectEnumerations.Priority.convertValue(value)
    case ConfigBase.Property.Attr.ASSIGNED => projectEnumerations.User.convertValue(value).map(userMapping.convert)
    case ConfigBase.Property.Attr.VERSION => projectEnumerations.Version.convertValue(value)
    case ConfigBase.Property.Attr.TRACKER => projectEnumerations.Tracker.convertValue(value)
    case ConfigBase.Property.Attr.CATEGORY => projectEnumerations.Category.convertValue(value)
    case _ => value
  }

  private def convertCf(projectEnumerations: ProjectEnumerations, name: String, value: Option[String]): Option[String] =
    RedmineUnmarshaller.customFieldDefinitions() match {
      case Some(customFields) =>
        val redmineCustomFieldDefinition: RedmineCustomFieldDefinition = customFields.find(customField => name.toInt == customField.id).get
        redmineCustomFieldDefinition.fieldFormat match {
          case "version" => projectEnumerations.Version.convertValue(value)
          case "user" => projectEnumerations.UserId.convertValue(projectEnumerations.User.convertValue(value))
          case _ => value
        }
      case None => None
    }

}

package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogComment, BacklogCommentDetail}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineJournal, RedmineJournalDetail}
import com.nulabinc.r2b.service.RedmineUnmarshaller
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class ConvertComments(pctx: ProjectContext) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def execute(projectIdentifier: String, issueId: Int, journals: Seq[RedmineJournal]): Seq[BacklogComment] =
    journalsToComments(projectIdentifier, issueId)(journals)

  private def journalsToComments(projectIdentifier: String, issueId: Int)(journals: Seq[RedmineJournal]) =
    journals.map(journalToComment(projectIdentifier, issueId)(_))

  private def journalToComment(projectIdentifier: String, issueId: Int)(journal: RedmineJournal) =
    getBacklogComment(projectIdentifier, issueId, journal)

  private def getBacklogComment(projectIdentifier: String, issueId: Int, redmineJournal: RedmineJournal): BacklogComment =
    BacklogComment(
      content = redmineJournal.notes + "\n" + getOtherProperty(projectIdentifier, issueId, redmineJournal.details),
      details = redmineJournal.details.filter(detail => !isOtherProperty(projectIdentifier, issueId, detail)).map(getBacklogCommentDetail(_)),
      createdUserId = redmineJournal.user.map(pctx.userMapping.convert),
      created = redmineJournal.createdOn)

  private def getBacklogCommentDetail(redmineJournalDetail: RedmineJournalDetail): BacklogCommentDetail =
    BacklogCommentDetail(
      property = redmineJournalDetail.property,
      name = convertName(redmineJournalDetail),
      oldValue = convertValue(redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.oldValue),
      newValue = convertValue(redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.newValue))

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

  private def convertName(detail: RedmineJournalDetail): String = detail.property match {
    case ConfigBase.Property.CF => pctx.getCustomFieldDefinitionsName(detail.name)
    case _ => convertBacklogName(detail.name)
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

  private def convertValue(property: String, name: String, value: Option[String]): Option[String] = property match {
    case ConfigBase.Property.ATTR => convertAttr(name, value)
    case ConfigBase.Property.CF => convertCf(name, value)
    case ConfigBase.Property.ATTACHMENT => value
    case "relation" => value
  }

  private def convertAttr(name: String, value: Option[String]): Option[String] = name match {
    case ConfigBase.Property.Attr.STATUS => pctx.getStatusName(value)
    case ConfigBase.Property.Attr.PRIORITY => pctx.getPriorityName(value)
    case ConfigBase.Property.Attr.ASSIGNED => pctx.getUserLoginId(value).map(pctx.userMapping.convert)
    case ConfigBase.Property.Attr.VERSION => pctx.getVersionName(value)
    case ConfigBase.Property.Attr.TRACKER => pctx.getTrackerName(value)
    case ConfigBase.Property.Attr.CATEGORY => pctx.getCategoryName(value)
    case _ => value
  }

  private def convertCf(name: String, value: Option[String]): Option[String] =
    RedmineUnmarshaller.customFieldDefinitions() match {
      case Some(customFields) =>
        val redmineCustomFieldDefinition: RedmineCustomFieldDefinition = customFields.find(customField => name.toInt == customField.id).get
        redmineCustomFieldDefinition.fieldFormat match {
          case "version" => pctx.getVersionName(value)
          case "user" => pctx.getUserFullname(value)
          case _ => value
        }
      case None => None
    }

}

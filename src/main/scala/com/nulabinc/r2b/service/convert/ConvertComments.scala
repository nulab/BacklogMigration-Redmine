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

  def execute(issueId: Int, journals: Seq[RedmineJournal]): Seq[BacklogComment] =
    journalsToComments(issueId)(journals)

  private def journalsToComments(issueId: Int)(journals: Seq[RedmineJournal]) =
    journals.map(journalToComment(issueId))

  private def journalToComment(issueId: Int)(journal: RedmineJournal) =
    getBacklogComment(issueId, journal)

  private def getBacklogComment(issueId: Int, redmineJournal: RedmineJournal): BacklogComment =
    BacklogComment(
      content = redmineJournal.notes + "\n" + getOtherProperty(issueId, redmineJournal.details),
      details = redmineJournal.details.filter(detail => !isOtherProperty(issueId, detail)).map(getBacklogCommentDetail),
      createdUserId = redmineJournal.user.map(pctx.userMapping.convert),
      created = redmineJournal.createdOn)

  private def getBacklogCommentDetail(redmineJournalDetail: RedmineJournalDetail): BacklogCommentDetail =
    BacklogCommentDetail(
      property = redmineJournalDetail.property,
      name = convertName(redmineJournalDetail),
      oldValue = convertValue(redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.oldValue),
      newValue = convertValue(redmineJournalDetail.property, redmineJournalDetail.name, redmineJournalDetail.newValue))

  private def getOtherProperty(issueId: Int, details: Seq[RedmineJournalDetail]): String =
    details.filter(isOtherProperty(issueId, _)).map(getOtherPropertyMessage(issueId, _)).mkString("\n")

  private def getOtherPropertyMessage(issueId: Int, detail: RedmineJournalDetail): String =
    if (isDoneRatioJournal(detail)) createMessage("label.done_ratio", detail)
    else if (isPrivateJournal(detail)) createMessage("label.private", detail)
    else if (isProjectId(detail)) createChangeProjectIdMessage("label.project", detail)
    else if (isRelationJournal(detail)) createMessage("label.relation", detail)
    else if (isAttachmentNotFound(issueId, detail: RedmineJournalDetail)) {
      if (detail.newValue.isDefined) Messages("message.add_attachment", detail.newValue.get)
      else if (detail.oldValue.isDefined) Messages("message.del_attachment", detail.oldValue.get)
      else ""
    }
    else ""

  private def createMessage(label: String, detail: RedmineJournalDetail): String =
    Messages("label.change_comment", Messages(label), getStringMessage(detail.oldValue), getStringMessage(detail.newValue))

  private def createChangeProjectIdMessage(label: String, detail: RedmineJournalDetail): String =
    Messages("label.change_comment", Messages(label), getProjectName(detail.oldValue), getProjectName(detail.newValue))

  private def getProjectName(value: Option[String]): String =
    if (value.isDefined && value.get != "") {
      pctx.getProjectName(value.get.toInt).getOrElse(Messages("label.not_set"))
    } else Messages("label.not_set")

  private def isOtherProperty(issueId: Int, detail: RedmineJournalDetail): Boolean =
    isRelationJournal(detail) || isDoneRatioJournal(detail) || isPrivateJournal(detail) || isProjectId(detail) ||
      isAttachmentNotFound(issueId, detail: RedmineJournalDetail)

  private def isAttachmentNotFound(issueId: Int, detail: RedmineJournalDetail): Boolean = {
    if (detail.property == ConfigBase.Property.ATTACHMENT) {
      val path: String = ConfigBase.Redmine.getIssueAttachmentDir(pctx.project.identifier, issueId, detail.name.toInt)
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

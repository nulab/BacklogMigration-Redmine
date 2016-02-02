package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogComment, BacklogCommentDetail}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineJournal, RedmineJournalDetail}
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

  private def getBacklogComment(issueId: Int, journal: RedmineJournal): BacklogComment = {
    val details = journal.details.filter(detail => !isNote(issueId, detail))
    val notes = journal.details.filter(isNote(issueId, _))
    BacklogComment(
      content = journal.notes + "\n" + getComment(issueId, notes),
      details = details.map(getBacklogCommentDetail),
      createdUserId = journal.user.map(pctx.userMapping.convert),
      created = journal.createdOn)
  }

  private def getBacklogCommentDetail(detail: RedmineJournalDetail): BacklogCommentDetail =
    BacklogCommentDetail(
      property = detail.property,
      name = convertName(detail),
      oldValue = convertValue(detail, detail.oldValue),
      newValue = convertValue(detail, detail.newValue))

  private def getComment(issueId: Int, notes: Seq[RedmineJournalDetail]): String =
    notes.map(getNote(issueId, _)).mkString("\n")

  private def getNote(issueId: Int, detail: RedmineJournalDetail): String =
    if (isDoneRatioJournal(detail)) getNote("label.done_ratio", detail)
    else if (isPrivateJournal(detail)) getNote("label.private", detail)
    else if (isRelationJournal(detail)) getNote("label.relation", detail)
    else if (isProjectId(detail)) getNoteForProject("label.project", detail)
    else if (isAnonymousUser(detail)) getNoteForUser("label.user", detail)
    else if (isAttachmentNotFound(issueId, detail: RedmineJournalDetail)) {
      if (detail.newValue.isDefined) Messages("message.add_attachment", detail.newValue.get)
      else if (detail.oldValue.isDefined) Messages("message.del_attachment", detail.oldValue.get)
      else ""
    }
    else ""

  private def getNote(label: String, detail: RedmineJournalDetail): String =
    Messages("label.change_comment", Messages(label), getValue(detail.oldValue), getValue(detail.newValue))

  private def getNoteForUser(label: String, detail: RedmineJournalDetail): String = {
    val oldValue = pctx.getUserFullname(detail.oldValue).orElse(detail.oldValue)
    val newValue = pctx.getUserFullname(detail.newValue).orElse(detail.newValue)
    Messages("label.change_comment", Messages(label), getValue(oldValue), getValue(newValue))
  }

  private def getNoteForProject(label: String, detail: RedmineJournalDetail): String =
    Messages("label.change_comment", Messages(label), getProjectName(detail.oldValue), getProjectName(detail.newValue))

  private def getProjectName(value: Option[String]): String =
    if (value.isDefined && value.get != "") {
      pctx.getProjectName(value.get.toInt).getOrElse(Messages("label.not_set"))
    } else Messages("label.not_set")

  private def isNote(issueId: Int, detail: RedmineJournalDetail): Boolean =
    isRelationJournal(detail) || isDoneRatioJournal(detail) ||
      isPrivateJournal(detail) || isProjectId(detail) ||
      isAnonymousUser(detail) ||
      isAttachmentNotFound(issueId, detail: RedmineJournalDetail)

  private def isAttachmentNotFound(issueId: Int, detail: RedmineJournalDetail): Boolean = {
    if (detail.property == ConfigBase.Property.ATTACHMENT) {
      val path: String = ConfigBase.Redmine.getIssueAttachmentDir(pctx.project.identifier, issueId, detail.name.toInt)
      !IOUtil.isDirectory(path)
    } else false
  }

  private def isAnonymousUser(detail: RedmineJournalDetail) = {
    if (detail.property == ConfigBase.Property.CUSTOM_FIELD) {
      val define: RedmineCustomFieldDefinition = pctx.customFieldDefinitions.find(customField => detail.name.toInt == customField.id).get
      if (define.fieldFormat == "user") {
        if (!(pctx.getUserFullname(detail.oldValue).isDefined && pctx.getUserFullname(detail.newValue).isDefined)) true
        else false
      } else false
    } else false
  }

  private def isRelationJournal(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.RELATION

  private def isDoneRatioJournal(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "done_ratio"

  private def isPrivateJournal(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "is_private"

  private def isProjectId(detail: RedmineJournalDetail) =
    detail.property == ConfigBase.Property.ATTR && detail.name == "project_id"

  private def getValue(value: Option[String]): String = value.getOrElse(Messages("label.not_set"))

  private def convertName(detail: RedmineJournalDetail): String = detail.property match {
    case ConfigBase.Property.CUSTOM_FIELD => pctx.getCustomFieldDefinitionsName(detail.name)
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

  private def convertValue(detail: RedmineJournalDetail, value: Option[String]): Option[String] = detail.property match {
    case ConfigBase.Property.ATTR => convertAttr(detail.name, value)
    case ConfigBase.Property.CUSTOM_FIELD => convertCf(detail.name, value)
    case ConfigBase.Property.ATTACHMENT => value
    case ConfigBase.Property.RELATION => value
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

  private def convertCf(name: String, value: Option[String]): Option[String] = {
    val define: RedmineCustomFieldDefinition = pctx.customFieldDefinitions.find(customField => name.toInt == customField.id).get
    define.fieldFormat match {
      case "version" => pctx.getVersionName(value)
      case "user" => pctx.getUserFullname(value)
      case _ => value
    }
  }

}

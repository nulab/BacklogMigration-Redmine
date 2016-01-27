package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.domain.{BacklogAttachment, BacklogCustomField, BacklogIssue}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.actor.utils.IssueTag
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.{RedmineAttachment, RedmineCustomField, RedmineCustomFieldDefinition, RedmineIssue}
import com.osinka.i18n.{Messages, Lang}

/**
  * @author uchida
  */
class ConvertIssue(pctx: ProjectContext) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def execute(redmineIssue: RedmineIssue, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition], redmineUrl: String): BacklogIssue = {
    val cc = new ConvertComments(pctx)
    BacklogIssue(
      id = redmineIssue.id,
      summary = redmineIssue.subject,
      parentIssueId = redmineIssue.parentIssueId.map(_.toLong),
      description = getDescription(redmineIssue),
      startDate = redmineIssue.startDate,
      dueDate = redmineIssue.dueDate,
      estimatedHours = redmineIssue.estimatedHours.map(_.toFloat),
      actualHours = redmineIssue.spentHours.map(_.toFloat),
      issueTypeName = redmineIssue.tracker,
      statusName = pctx.statusMapping.convert(redmineIssue.status),
      categoryName = redmineIssue.category,
      versionName = redmineIssue.version,
      priorityName = pctx.priorityMapping.convert(redmineIssue.priority),
      assigneeUserId = redmineIssue.assigneeId.map(pctx.userMapping.convert),
      attachments = getBacklogAttachments(redmineIssue.attachments),
      comments = cc.execute(redmineIssue.id, redmineIssue.journals),
      customFields = getBacklogCustomFields(redmineIssue.customFields, redmineCustomFieldDefinitions),
      createdUserId = redmineIssue.author.map(pctx.userMapping.convert),
      created = redmineIssue.createdOn,
      updatedUserId = redmineIssue.author.map(pctx.userMapping.convert),
      updated = redmineIssue.updatedOn)
  }

  private def getDescription(redmineIssue: RedmineIssue): String = {
    val sb = new StringBuilder
    sb.append(redmineIssue.description)
    sb.append("\n").append(Messages("label.done_ratio")).append(":").append(redmineIssue.doneRatio)
    sb.append("\n").append(IssueTag.getTag(redmineIssue.id, pctx.conf.redmineUrl))
    sb.result()
  }

  private def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
    attachments.map(getBacklogAttachment)

  private def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
    BacklogAttachment(attachment.id, attachment.fileName)

  private def getBacklogCustomFields(redmineCustomFields: Seq[RedmineCustomField], redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Seq[BacklogCustomField] =
    redmineCustomFields.map(redmineCustomField => getBacklogCustomField(redmineCustomField, redmineCustomFieldDefinitions))

  private def getBacklogCustomField(redmineCustomField: RedmineCustomField, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): BacklogCustomField =
    BacklogCustomField(
      id = redmineCustomField.id,
      name = redmineCustomField.name,
      value = getCustomFieldValue(redmineCustomField, redmineCustomFieldDefinitions),
      multiple = redmineCustomField.multiple,
      values = redmineCustomField.values)

  private def getCustomFieldValue(redmineCustomField: RedmineCustomField, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Option[String] = {
    val redmineCustomFieldDefinition: RedmineCustomFieldDefinition = redmineCustomFieldDefinitions.find(_.id == redmineCustomField.id).get
    val fieldFormat: String = redmineCustomFieldDefinition.fieldFormat
    fieldFormat match {
      case ConfigBase.FieldFormat.VERSION => pctx.getCategoryName(redmineCustomField.value)
      case ConfigBase.FieldFormat.USER => pctx.getMembershipUserName(redmineCustomField.value)
      case ConfigBase.FieldFormat.INT | ConfigBase.FieldFormat.FLOAT =>
        redmineCustomField.value.orElse(redmineCustomFieldDefinition.defaultValue)
      case _ => redmineCustomField.value
    }
  }

}

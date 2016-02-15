package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.domain.{BacklogAttachment, BacklogCustomField, BacklogIssue}
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.actor.utils.IssueTag
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.{RedmineAttachment, RedmineCustomField, RedmineIssue}
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class ConvertIssue(pctx: ProjectContext) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def execute(redmineIssue: RedmineIssue): BacklogIssue = {
    val cc = new ConvertComments(pctx, redmineIssue.id)
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
      comments = cc.execute(redmineIssue.journals),
      customFields = getBacklogCustomFields(redmineIssue.customFields),
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

  private def getBacklogCustomFields(redmineCustomFields: Seq[RedmineCustomField]): Seq[BacklogCustomField] =
    redmineCustomFields.map(redmineCustomField => getBacklogCustomField(redmineCustomField))

  private def getBacklogCustomField(redmineCustomField: RedmineCustomField): BacklogCustomField =
    BacklogCustomField(
      id = redmineCustomField.id,
      name = redmineCustomField.name,
      value = getCustomFieldValue(redmineCustomField),
      multiple = redmineCustomField.multiple,
      values = redmineCustomField.values)

  private def getCustomFieldValue(redmineCustomField: RedmineCustomField): Option[String] = {
    val customFieldDefinition = pctx.customFieldDefinitions.find(_.id == redmineCustomField.id).get

    customFieldDefinition.fieldFormat match {
      case ConfigBase.FieldFormat.VERSION => pctx.getCategoryName(redmineCustomField.value)
      case ConfigBase.FieldFormat.USER => pctx.getMembershipUserName(redmineCustomField.value)
      case ConfigBase.FieldFormat.INT | ConfigBase.FieldFormat.FLOAT =>
        redmineCustomField.value.orElse(customFieldDefinition.defaultValue)
      case _ => redmineCustomField.value
    }
  }

}

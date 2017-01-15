package com.nulabinc.r2b.service.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.{RedmineDirectory, RedmineProperty}
import com.nulabinc.r2b.domain.{RedmineAttachment, RedmineCustomField, RedmineIssue}
import com.nulabinc.r2b.service._
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class ConvertIssueServiceImpl @Inject()(
                                         redmineDirectory: RedmineDirectory,
                                         propertyService: PropertyService,
                                         statusMapping: ConvertStatusMapping,
                                         priorityMapping: ConvertPriorityMapping,
                                         userMapping: ConvertUserMapping,
                                         convertCommentService: ConvertCommentService) extends ConvertIssueService with Logging {

  override def convert(redmineIssue: RedmineIssue): BacklogIssue = {
    val categoryNames = redmineIssue.category match {
      case Some(name) => Seq(name)
      case _ => Seq.empty[String]
    }

    val milestoneNames = redmineIssue.version match {
      case Some(name) => Seq(name)
      case _ => Seq.empty[String]
    }

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
      statusName = statusMapping.convert(redmineIssue.status),
      categoryNames = categoryNames,
      versionNames = Seq.empty[String],
      milestoneNames = milestoneNames,
      priorityName = priorityMapping.convert(redmineIssue.priority),
      assigneeUserId = redmineIssue.assigneeId.map(userMapping.convert),
      attachments = getBacklogAttachments(redmineIssue.attachments),
      comments = convertCommentService.convert(redmineIssue.journals, redmineIssue.id),
      sharedFiles = Seq.empty[BacklogSharedFile],
      customFields = getBacklogCustomFields(redmineIssue.customFields),
      notifiedUserIds = Seq.empty[String],
      operation = getBacklogOperation(redmineIssue)
    )
  }

  private[this] def getBacklogOperation(redmineIssue: RedmineIssue): BacklogOperation =
    BacklogOperation(
      createdUserId = redmineIssue.author.map(userMapping.convert),
      created = redmineIssue.createdOn,
      updatedUserId = redmineIssue.author.map(userMapping.convert),
      updated = redmineIssue.updatedOn)

  private[this] def getDescription(redmineIssue: RedmineIssue): String = {
    val sb = new StringBuilder
    sb.append(redmineIssue.description)
    sb.append("\n").append(Messages("common.done_ratio")).append(":").append(redmineIssue.doneRatio)
    sb.result()
  }

  private[this] def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
    attachments.map(getBacklogAttachment)

  private[this] def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
    BacklogAttachment(attachment.id, attachment.fileName)

  private[this] def getBacklogCustomFields(redmineCustomFields: Seq[RedmineCustomField]): Seq[BacklogCustomField] =
    redmineCustomFields.map(redmineCustomField => getBacklogCustomField(redmineCustomField))

  private[this] def getBacklogCustomField(redmineCustomField: RedmineCustomField): BacklogCustomField =
    BacklogCustomField(
      name = redmineCustomField.name,
      value = getCustomFieldValue(redmineCustomField),
      multiple = redmineCustomField.multiple,
      values = redmineCustomField.values)

  private[this] def getCustomFieldValue(redmineCustomField: RedmineCustomField): Option[String] =
    (for {
      customFieldDefinitions <- RedmineUnmarshaller.customFieldDefinitions(redmineDirectory)
      customFieldDefinition <- customFieldDefinitions.find(_.id == redmineCustomField.id)
    } yield {
      customFieldDefinition.fieldFormat match {
        case RedmineProperty.FieldFormat.VERSION => propertyService.optVersionName(redmineCustomField.value)
        case RedmineProperty.FieldFormat.USER => propertyService.optMembershipName(redmineCustomField.value)
        case RedmineProperty.FieldFormat.INT | RedmineProperty.FieldFormat.FLOAT =>
          redmineCustomField.value.orElse(customFieldDefinition.defaultValue)
        case RedmineProperty.FieldFormat.BOOL =>
          redmineCustomField.value match {
            case Some("0") => Some(Messages("common.no"))
            case Some("1") => Some(Messages("common.yes"))
            case _ => None
          }
        case _ => redmineCustomField.value
      }
    }).flatten

}

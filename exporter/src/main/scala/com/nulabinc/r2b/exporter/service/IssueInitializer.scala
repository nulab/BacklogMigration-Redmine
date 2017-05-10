package com.nulabinc.r2b.exporter.service

import java.io.{FileOutputStream, InputStream}
import java.net.URL
import java.nio.channels.Channels

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{DateUtil, IOUtil, Logging, StringUtil}
import com.nulabinc.r2b.exporter.convert._
import com.nulabinc.r2b.mapping.core.{ConvertPriorityMapping, ConvertUserMapping}
import com.nulabinc.r2b.redmine.conf.{RedmineApiConfiguration, RedmineConstantValue}
import com.nulabinc.r2b.redmine.domain.{PropertyValue, RedmineCustomFieldDefinition}
import com.taskadapter.redmineapi.bean._

import scala.collection.JavaConverters._
import scalax.file.Path

/**
  * @author uchida
  */
class IssueInitializer(apiConfig: RedmineApiConfiguration,
                       backlogPaths: BacklogPaths,
                       propertyValue: PropertyValue,
                       issueDirPath: Path,
                       journals: Seq[Journal],
                       attachments: Seq[Attachment],
                       issueWrites: IssueWrites,
                       userWrites: UserWrites,
                       customFieldWrites: CustomFieldWrites,
                       customFieldValueWrites: CustomFieldValueWrites,
                       attachmentWrites: AttachmentWrites)
    extends Logging {

  val userMapping     = new ConvertUserMapping()
  val priorityMapping = new ConvertPriorityMapping()

  def initialize(issue: Issue): BacklogIssue = {
    //attachments
    val attachmentFilter    = new AttachmentFilter(journals)
    val filteredAttachments = attachmentFilter.filter(attachments)
    val backlogAttachments  = filteredAttachments.map(Convert.toBacklog(_)(attachmentWrites))
    filteredAttachments.foreach(attachment)

    val backlogIssue: BacklogIssue = Convert.toBacklog(issue)(issueWrites)
    backlogIssue.copy(
      summary = summary(issue),
      optParentIssueId = parentIssueId(issue),
      description = description(issue),
      optStartDate = startDate(issue),
      optDueDate = dueDate(issue),
      optEstimatedHours = estimatedHours(issue),
      optIssueTypeName = issueTypeName(issue),
      categoryNames = categoryNames(issue),
      milestoneNames = milestoneNames(issue),
      priorityName = priorityName(issue),
      optAssignee = assignee(issue),
      customFields = issue.getCustomFields.asScala.toSeq.flatMap(customField),
      attachments = backlogAttachments,
      notifiedUsers = Seq.empty[BacklogUser]
    )
  }

  private[this] def summary(issue: Issue): BacklogIssueSummary = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.SUBJECT)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => BacklogIssueSummary(value = Option(detail.getOldValue).getOrElse(""), original = issue.getSubject)
      case None         => BacklogIssueSummary(value = issue.getSubject, original = issue.getSubject)
    }
  }

  private[this] def parentIssueId(issue: Issue): Option[Long] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.PARENT)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        Option(detail.getOldValue) match {
          case Some(value) if (value.nonEmpty) =>
            StringUtil.safeStringToInt(value) match {
              case Some(intValue) => Some(intValue)
              case _              => None
            }
          case _ => None
        }
      case None => Option(issue.getParentId).map(_.intValue())
    }
  }

  private[this] def description(issue: Issue): String = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.DESCRIPTION)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => Option(detail.getOldValue).getOrElse("")
      case None         => issue.getDescription
    }
  }

  private[this] def startDate(issue: Issue): Option[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.START_DATE)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => Option(detail.getOldValue)
      case None         => Option(issue.getStartDate).map(DateUtil.dateFormat)
    }
  }

  private[this] def dueDate(issue: Issue): Option[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.DUE_DATE)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => Option(detail.getOldValue)
      case None         => Option(issue.getDueDate).map(DateUtil.dateFormat)
    }
  }

  private[this] def estimatedHours(issue: Issue): Option[Float] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.ESTIMATED_HOURS)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => Option(detail.getOldValue).filter(_.nonEmpty).map(_.toFloat)
      case None         => Option(issue.getEstimatedHours).map(_.toFloat)
    }
  }

  private[this] def issueTypeName(issue: Issue): Option[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.TRACKER)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        propertyValue.trackerOfId(Option(detail.getOldValue)).map(_.getName)
      case None => Option(issue.getTracker).map(_.getName)
    }
  }

  private[this] def categoryNames(issue: Issue): Seq[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.CATEGORY)
    val optDetails        = issueInitialValue.findJournalDetails(journals)
    optDetails match {
      case Some(details) =>
        details.flatMap { detail =>
          propertyValue.categoryOfId(Option(detail.getOldValue)).map(_.getName)
        }
      case _ => Option(issue.getCategory).map(_.getName).toSeq
    }
  }

  private[this] def milestoneNames(issue: Issue): Seq[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.VERSION)
    val optDetails        = issueInitialValue.findJournalDetails(journals)
    optDetails match {
      case Some(details) =>
        details.flatMap { detail =>
          propertyValue.versionOfId(Option(detail.getOldValue)).map(_.getName)
        }
      case _ => Option(issue.getTargetVersion).map(_.getName).toSeq
    }
  }

  private[this] def priorityName(issue: Issue): String = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.PRIORITY)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        propertyValue.priorityOfId(Option(detail.getOldValue)).map(_.getName).map(priorityMapping.convert).getOrElse("")
      case None => priorityMapping.convert(issue.getPriorityText)
    }
  }

  private[this] def assignee(issue: Issue): Option[BacklogUser] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.ASSIGNED)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        propertyValue.userOfId(Option(detail.getOldValue)).map(Convert.toBacklog(_)(userWrites))
      case None => Option(issue.getAssignee).map(Convert.toBacklog(_)(userWrites))
    }
  }

  private[this] def customField(customField: CustomField): Option[BacklogCustomField] = {
    val optCustomFieldDefinition = propertyValue.customFieldDefinitionOfName(customField.getName)
    optCustomFieldDefinition match {
      case Some(customFieldDefinition) =>
        if (customFieldDefinition.isMultiple) multipleCustomField(customField, customFieldDefinition)
        else singleCustomField(customField, customFieldDefinition)
      case _ => None
    }
  }

  private[this] def multipleCustomField(customField: CustomField, customFieldDefinition: RedmineCustomFieldDefinition): Option[BacklogCustomField] = {
    val issueInitialValue                      = new IssueInitialValue(RedmineConstantValue.CUSTOM_FIELD, customFieldDefinition.id.toString)
    val optDetails: Option[Seq[JournalDetail]] = issueInitialValue.findJournalDetails(journals)
    val initialValues: Seq[String] =
      optDetails match {
        case Some(details) =>
          details.flatMap(detail => Convert.toBacklog((customField.getId.toString, Option(detail.getOldValue)))(customFieldValueWrites))
        case _ => customField.getValues.asScala
      }
    Convert.toBacklog(customField)(customFieldWrites) match {
      case Some(backlogCustomField) => Some(backlogCustomField.copy(values = initialValues))
      case _                        => None
    }
  }

  private[this] def singleCustomField(customField: CustomField, customFieldDefinition: RedmineCustomFieldDefinition): Option[BacklogCustomField] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.CUSTOM_FIELD, customFieldDefinition.id.toString)
    val initialValue: Option[String] =
      issueInitialValue.findJournalDetail(journals) match {
        case Some(detail) => Convert.toBacklog((customField.getId.toString, Option(detail.getOldValue)))(customFieldValueWrites)
        case _            => Convert.toBacklog((customField.getId.toString, Option(customField.getValue)))(customFieldValueWrites)
      }
    Convert.toBacklog(customField)(customFieldWrites) match {
      case Some(backlogCustomField) => Some(backlogCustomField.copy(optValue = initialValue))
      case _                        => None
    }
  }

  private[this] def attachment(attachment: Attachment) = {
    val url: URL = new URL(s"${attachment.getContentURL}?key=${apiConfig.key}")
    download(attachment.getFileName, url.openStream())
  }

  private[this] def download(name: String, content: InputStream) = {
    val dir  = backlogPaths.issueAttachmentDirectoryPath(issueDirPath)
    val path = backlogPaths.issueAttachmentPath(dir, name)
    IOUtil.createDirectory(dir)
    val rbc = Channels.newChannel(content)
    val fos = new FileOutputStream(path.path)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

    rbc.close()
    fos.close()
  }

}

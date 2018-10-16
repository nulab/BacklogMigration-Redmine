package com.nulabinc.backlog.r2b.exporter.service

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{DateUtil, IOUtil, Logging, StringUtil}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.nulabinc.backlog.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.backlog.r2b.redmine.domain.RedmineCustomFieldDefinition
import com.taskadapter.redmineapi.bean._

import scala.collection.JavaConverters._
import better.files.{File => Path}
import com.nulabinc.backlog.r2b.utils.TextileUtil

/**
  * @author uchida
  */
private[exporter] class IssueInitializer(exportContext: ExportContext, issueDirPath: Path, journals: Seq[Journal], attachments: Seq[Attachment], backlogTextFormattingRule: BacklogTextFormattingRule)
    extends Logging {

  implicit val issueWrites            = exportContext.issueWrites
  implicit val attachmentWrites       = exportContext.attachmentWrites
  implicit val userWrites             = exportContext.userWrites
  implicit val customFieldWrites      = exportContext.customFieldWrites
  implicit val customFieldValueWrites = exportContext.customFieldValueWrites

  def initialize(issue: Issue): BacklogIssue = {
    //attachments
    val attachmentFilter    = new AttachmentFilter(journals)
    val filteredAttachments = attachmentFilter.filter(attachments)
    val backlogAttachments  = filteredAttachments.map(Convert.toBacklog(_))
    filteredAttachments.foreach(attachment)

    val backlogIssue: BacklogIssue = Convert.toBacklog(issue)
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
      case None         => TextileUtil.convert(issue.getDescription, backlogTextFormattingRule)
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
        exportContext.propertyValue.trackerOfId(Option(detail.getOldValue)).map(_.getName)
      case None => Option(issue.getTracker).map(_.getName)
    }
  }

  private[this] def categoryNames(issue: Issue): Seq[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.CATEGORY)
    val optDetails        = issueInitialValue.findJournalDetails(journals)
    optDetails match {
      case Some(details) =>
        details.flatMap { detail =>
          exportContext.propertyValue.categoryOfId(Option(detail.getOldValue)).map(_.getName)
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
          exportContext.propertyValue.versionOfId(Option(detail.getOldValue)).map(_.getName)
        }
      case _ => Option(issue.getTargetVersion).map(_.getName).toSeq
    }
  }

  private[this] def priorityName(issue: Issue): String = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.PRIORITY)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        exportContext.propertyValue
          .priorityOfId(Option(detail.getOldValue))
          .map(_.getName)
          .map(exportContext.mappingPriorityService.convert)
          .getOrElse("")
      case None => exportContext.mappingPriorityService.convert(issue.getPriorityText)
    }
  }

  private[this] def assignee(issue: Issue): Option[BacklogUser] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.ASSIGNED)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) =>
        exportContext.propertyValue.userOfId(Option(detail.getOldValue)).map(Convert.toBacklog(_))
      case None => Option(issue.getAssignee).map(Convert.toBacklog(_))
    }
  }

  private[this] def customField(customField: CustomField): Option[BacklogCustomField] = {
    val optCustomFieldDefinition = exportContext.propertyValue.customFieldDefinitionOfName(customField.getName)
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
          details.flatMap(detail => Convert.toBacklog((customField.getId.toString, Option(detail.getOldValue))))
        case _ => customField.getValues.asScala
      }
    Convert.toBacklog(customField) match {
      case Some(backlogCustomField) => Some(backlogCustomField.copy(values = initialValues))
      case _                        => None
    }
  }

  private[this] def singleCustomField(customField: CustomField, customFieldDefinition: RedmineCustomFieldDefinition): Option[BacklogCustomField] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.CUSTOM_FIELD, customFieldDefinition.id.toString)
    val initialValue: Option[String] =
      issueInitialValue.findJournalDetail(journals) match {
        case Some(detail) => Convert.toBacklog((customField.getId.toString, Option(detail.getOldValue)))
        case _            => Convert.toBacklog((customField.getId.toString, Option(customField.getValue)))
      }
    Convert.toBacklog(customField) match {
      case Some(backlogCustomField) => Some(backlogCustomField.copy(optValue = initialValue))
      case _                        => None
    }
  }

  private[this] def attachment(attachment: Attachment): Unit = {

    val dir  = exportContext.backlogPaths.issueAttachmentDirectoryPath(issueDirPath)
    val path = exportContext.backlogPaths.issueAttachmentPath(dir, attachment.getFileName)
    IOUtil.createDirectory(dir)

    val url: URL = new URL(s"${attachment.getContentURL}?key=${exportContext.apiConfig.key}")

    try {
      val rbc = Channels.newChannel(url.openStream())
      val fos = new FileOutputStream(path.path.toFile)
      fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

      rbc.close()
      fos.close()
    } catch {
      case e: Throwable => logger.warn("Download issue attachment failed: " + e.getMessage)
    }
  }

}

package com.nulabinc.r2b.exporter.service

import com.nulabinc.backlog.migration.converter.Convert
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.{DateUtil, Logging, StringUtil}
import com.nulabinc.r2b.exporter.convert.{IssueWrites, UserWrites}
import com.nulabinc.r2b.mapping.core.ConvertPriorityMapping
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.domain.PropertyValue
import com.taskadapter.redmineapi.bean.{Issue, Journal}

/**
  * @author uchida
  */
class IssueInitializer(issueWrites: IssueWrites, userWrites: UserWrites, journals: Seq[Journal], propertyValue: PropertyValue) extends Logging {

  val priorityMapping = new ConvertPriorityMapping()

  def initialize(issue: Issue): BacklogIssue = {
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
      //customFields = backlogIssue.customFields.map(customField),
      notifiedUsers = Seq.empty[BacklogUser]
    )
  }

  private[this] def summary(issue: Issue): String = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.SUBJECT)
    issueInitialValue.findJournalDetail(journals) match {
      case Some(detail) => Option(detail.getOldValue).getOrElse("")
      case None         => issue.getSubject
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
    val details           = issueInitialValue.findJournalDetails(journals)
    if (details.isEmpty) Option(issue.getCategory).map(_.getName).toSeq
    else
      details.flatMap { detail =>
        propertyValue.categoryOfId(Option(detail.getOldValue)).map(_.getName)
      }
  }

  private[this] def milestoneNames(issue: Issue): Seq[String] = {
    val issueInitialValue = new IssueInitialValue(RedmineConstantValue.ATTR, RedmineConstantValue.Attr.VERSION)
    val details           = issueInitialValue.findJournalDetails(journals)
    if (details.isEmpty) Option(issue.getTargetVersion).map(_.getName).toSeq
    else
      details.flatMap { detail =>
        propertyValue.versionOfId(Option(detail.getOldValue)).map(_.getName)
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

  //TODO
//  private[this] def customField(customField: CustomField): CustomField =
//    if (customField.fieldTypeId == BacklogConstantValue.CustomField.MultipleList || customField.fieldTypeId == BacklogConstantValue.CustomField.CheckBox) {
//      val issueInitialValue          = new IssueInitialValue(customField.name)
//      val details                    = issueInitialValue.findJournalDetails(journals)
//      val initialValues: Seq[String] = if (details.isEmpty) customField.values else details.flatMap(_.optOriginalValue)
//      customField.copy(values = initialValues)
//    } else {
//      val issueInitialValue = new IssueInitialValue(customField.name)
//      val initialValue: Option[String] =
//        issueInitialValue.findJournalDetail(journals).map(_.optOriginalValue).getOrElse(customField.optValue)
//      customField.copy(optValue = initialValue)
//    }

}

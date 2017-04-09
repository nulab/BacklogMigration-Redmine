package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.{Convert, Writes}
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.DateUtil
import com.nulabinc.r2b.mapping.core.{ConvertPriorityMapping, ConvertStatusMapping}
import com.taskadapter.redmineapi.bean.Issue

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueWrites @Inject()(implicit val attachmentWrites: AttachmentWrites,
                            implicit val userWrites: UserWrites,
                            implicit val customFieldWrites: CustomFieldWrites)
    extends Writes[Issue, BacklogIssue] {

  val statusMapping   = new ConvertStatusMapping()
  val priorityMapping = new ConvertPriorityMapping()

  override def writes(issue: Issue): BacklogIssue = {
    BacklogIssue(
      eventType = "issue",
      originalSummary = issue.getSubject,
      id = issue.getId.intValue(),
      optIssueKey = None,
      summary = issue.getSubject,
      optParentIssueId = parentIssueId(issue),
      description = issue.getDescription,
      optStartDate = Option(issue.getStartDate).map(DateUtil.dateFormat),
      optDueDate = Option(issue.getDueDate).map(DateUtil.dateFormat),
      optEstimatedHours = Option(issue.getEstimatedHours).map(_.floatValue()),
      optActualHours = Option(issue.getSpentHours).map(_.floatValue()),
      optIssueTypeName = Some(issue.getTracker.getName),
      statusName = statusMapping.convert(issue.getStatusName),
      categoryNames = Seq(issue.getCategory.getName),
      versionNames = Seq.empty[String],
      milestoneNames = Seq(issue.getTargetVersion.getName),
      priorityName = priorityMapping.convert(issue.getPriorityText),
      optAssignee = Option(issue.getAssignee).map(Convert.toBacklog(_)),
      sharedFiles = Seq.empty[BacklogSharedFile],
      customFields = issue.getCustomFields.asScala.toSeq.flatMap(Convert.toBacklog(_)),
      notifiedUsers = Seq.empty[BacklogUser],
      operation = toBacklogOperation(issue)
    )
  }

  private[this] def parentIssueId(issue: Issue): Option[Long] = {
    Option(issue.getParentId.intValue()) match {
      case Some(id) if id == 0 => None
      case Some(id)            => Some(id)
      case _                   => None
    }
  }

  private[this] def toBacklogOperation(issue: Issue): BacklogOperation =
    BacklogOperation(
      optCreatedUser = Option(issue.getAuthor).map(Convert.toBacklog(_)),
      optCreated = Option(issue.getCreatedOn).map(DateUtil.isoFormat),
      optUpdatedUser = Option(issue.getAuthor).map(Convert.toBacklog(_)),
      optUpdated = Option(issue.getUpdatedOn).map(DateUtil.isoFormat)
    )

}

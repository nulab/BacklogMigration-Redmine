package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueType
import com.taskadapter.redmineapi.bean.Tracker

/**
  * @author uchida
  */
private[exporter] class IssueTypesWrites @Inject() () extends Writes[Seq[Tracker], Seq[BacklogIssueType]] {

  override def writes(trackers: Seq[Tracker]): Seq[BacklogIssueType] = {
    trackers.map(toBacklog)
  }

  private[this] def toBacklog(tracker: Tracker) = {
    BacklogIssueType(
      optId = Some(tracker.getId.intValue()),
      name = tracker.getName,
      color = BacklogConstantValue.ISSUE_TYPE_COLOR.getStrValue,
      delete = false
    )
  }

}

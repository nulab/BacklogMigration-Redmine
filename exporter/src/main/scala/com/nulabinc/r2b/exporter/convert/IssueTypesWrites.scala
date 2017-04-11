package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogIssueType
import com.taskadapter.redmineapi.bean.Tracker

/**
  * @author uchida
  */
class IssueTypesWrites @Inject()() extends Writes[Seq[Tracker], Seq[BacklogIssueType]] {

  override def writes(trackers: Seq[Tracker]): Seq[BacklogIssueType] = {
    trackers.map(toBacklog)
  }

  private[this] def toBacklog(tracker: Tracker) = {
    BacklogIssueType(optId = Some(tracker.getId.intValue()),
                     name = tracker.getName,
                     color = BacklogConstantValue.ISSUE_TYPE_COLOR.getStrValue,
                     delete = false)
  }

}
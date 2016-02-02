package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogIssueType, BacklogIssueTypesWrapper}
import com.nulabinc.r2b.domain.RedmineTracker

/**
  * @author uchida
  */
object ConvertIssueTypes {

  def apply(trackers: Seq[RedmineTracker]): BacklogIssueTypesWrapper = {
    val backlogIssueTypes: Seq[BacklogIssueType] = trackers.map(getBacklogIssueType)
    BacklogIssueTypesWrapper(backlogIssueTypes)
  }

  private def getBacklogIssueType(tracker: RedmineTracker): BacklogIssueType =
    BacklogIssueType(name = tracker.name, color = BacklogConfigBase.Backlog.ISSUE_TYPE_COLOR.getStrValue)

}
package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssuePriority

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssuePriorityServiceImpl @Inject()(redmine: RedmineManager) extends IssuePriorityService with Logging {

  override def allIssuePriorities(): Seq[IssuePriority] = {
    try {
      redmine.getIssueManager.getIssuePriorities.asScala
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssuePriority]
    }
  }

}

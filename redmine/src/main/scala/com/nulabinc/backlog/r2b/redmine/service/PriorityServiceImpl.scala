package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssuePriority

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class PriorityServiceImpl @Inject()(redmine: RedmineManager) extends PriorityService with Logging {

  override def allPriorities(): Seq[IssuePriority] = {
    try {
      redmine.getIssueManager.getIssuePriorities.asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssuePriority]
    }
  }

}

package com.nulabinc.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssueStatus

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(redmine: RedmineManager) extends StatusService with Logging {

  override def allStatuses(): Seq[IssueStatus] = {
    try {
      redmine.getIssueManager.getStatuses.asScala
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[IssueStatus]
    }
  }

}

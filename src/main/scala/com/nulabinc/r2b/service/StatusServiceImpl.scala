package com.nulabinc.r2b.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.Logging
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
        log.error(e)
        Seq.empty[IssueStatus]
    }
  }

}

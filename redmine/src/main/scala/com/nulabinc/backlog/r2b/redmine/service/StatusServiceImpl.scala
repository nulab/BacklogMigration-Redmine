package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssueStatus

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject() (redmine: RedmineManager)
    extends StatusService
    with Logging {

  override def allStatuses(): Seq[IssueStatus] = {
    try {
      redmine.getIssueManager.getStatuses.asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssueStatus]
    }
  }

}

package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Tracker

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class TrackerServiceImpl @Inject() (redmine: RedmineManager)
    extends TrackerService
    with Logging {

  override def allTrackers(): Seq[Tracker] = {
    try {
      redmine.getIssueManager.getTrackers.asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[Tracker]
    }
  }

}

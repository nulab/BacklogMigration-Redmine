package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Tracker

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class TrackerServiceImpl @Inject()(redmine: RedmineManager) extends TrackerService with Logging {

  override def allTrackers(): Seq[Tracker] = {
    try {
      redmine.getIssueManager.getTrackers.asScala
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[Tracker]
    }
  }

}

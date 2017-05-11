package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Version

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class VersionServiceImpl @Inject()(@Named("projectId") projectId: Int, redmine: RedmineManager) extends VersionService with Logging {

  override def allVersions(): Seq[Version] =
    try {
      redmine.getProjectManager.getVersions(projectId).asScala
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[Version]
    }

}

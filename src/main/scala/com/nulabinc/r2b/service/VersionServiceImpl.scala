package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
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
        log.error(e)
        Seq.empty[Version]
    }

}

package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.domain.RedmineProjectId
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Version

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class VersionServiceImpl @Inject()(projectId: RedmineProjectId, redmine: RedmineManager) extends VersionService with Logging {

  override def allVersions(): Seq[Version] =
    try {
      redmine.getProjectManager.getVersions(projectId.value).asScala
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[Version]
    }

}

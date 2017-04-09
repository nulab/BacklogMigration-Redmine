package com.nulabinc.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Project

/**
  * @author uchida
  */
class ProjectServiceImpl @Inject()(apiConfig: RedmineConfig, redmine: RedmineManager) extends ProjectService with Logging {

  override def project(): Project =
    redmine.getProjectManager.getProjectByKey(apiConfig.projectKey)

  override def optProject(): Option[Project] =
    try {
      Some(redmine.getProjectManager.getProjectByKey(apiConfig.projectKey))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
}

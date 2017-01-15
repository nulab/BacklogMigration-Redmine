package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Project

/**
  * @author uchida
  */
class ProjectServiceImpl @Inject()(@Named("projectKey") projectKey: String, redmine: RedmineManager) extends ProjectService with Logging {

  override def project(): Project =
    redmine.getProjectManager.getProjectByKey(projectKey)

  override def optProject(): Option[Project] =
    try {
      Some(redmine.getProjectManager.getProjectByKey(projectKey))
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
        None
    }
}

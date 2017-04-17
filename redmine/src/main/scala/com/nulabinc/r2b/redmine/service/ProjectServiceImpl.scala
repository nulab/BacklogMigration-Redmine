package com.nulabinc.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Project

/**
  * @author uchida
  */
class ProjectServiceImpl @Inject()(redmine: RedmineManager) extends ProjectService with Logging {

  override def tryProjectOfKey(projectKey: String): Project =
    redmine.getProjectManager.getProjectByKey(projectKey)

  override def optProjectOfId(id: Int): Option[Project] = {
    try {
      Some(redmine.getProjectManager.getProjectById(id))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

  override def optProjectOfKey(projectKey: String): Option[Project] =
    try {
      Some(redmine.getProjectManager.getProjectByKey(projectKey))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }

}

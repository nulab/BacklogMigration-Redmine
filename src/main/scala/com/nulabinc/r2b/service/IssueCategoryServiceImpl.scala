package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssueCategory

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueCategoryServiceImpl @Inject()(@Named("projectId") projectId: Int, redmine: RedmineManager) extends IssueCategoryService with Logging {

  override def allCategories(): Seq[IssueCategory] =
    try {
      redmine.getIssueManager.getCategories(projectId).asScala
    } catch {
      case e: Throwable =>
        log.error(e)
        Seq.empty[IssueCategory]
    }

}

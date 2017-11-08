package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.domain.RedmineProjectId
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssueCategory

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueCategoryServiceImpl @Inject()(projectId: RedmineProjectId, redmine: RedmineManager) extends IssueCategoryService with Logging {

  override def allCategories(): Seq[IssueCategory] =
    try {
      redmine.getIssueManager.getCategories(projectId.value).asScala
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssueCategory]
    }

}

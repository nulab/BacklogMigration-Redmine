package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.News

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class NewsServiceImpl @Inject()(@Named("projectKey") projectKey: String, redmine: RedmineManager) extends NewsService with Logging {

  override def allNews(): Seq[News] =
    try {
      redmine.getProjectManager.getNews(projectKey).asScala
    } catch {
      case e: Throwable =>
        log.error(e)
        Seq.empty[News]
    }

}

package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.News

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class NewsServiceImpl @Inject()(apiConfig: RedmineApiConfiguration, redmine: RedmineManager) extends NewsService with Logging {

  override def allNews(): Seq[News] =
    try {
      redmine.getProjectManager.getNews(apiConfig.projectKey).asScala.toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[News]
    }

}

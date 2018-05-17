package com.nulabinc.backlog.r2b.redmine.service

import java.net.URISyntaxException

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.{WikiPage, WikiPageDetail}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class WikiServiceImpl @Inject()(apiConfig: RedmineApiConfiguration, redmine: RedmineManager) extends WikiService with Logging {

  override def allWikis(): Seq[WikiPage] =
    try {
      redmine.getWikiManager.getWikiPagesByProject(apiConfig.projectKey).asScala
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[WikiPage]
    }

  override def optWikiDetail(pageTitle: String): Option[WikiPageDetail] = {
    logger.debug("Get a wiki Title: " + pageTitle)
    try {
      val wiki = redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(apiConfig.projectKey, pageTitle)
      Some(wiki)
    } catch {
      case e: URISyntaxException =>
        logger.warn(s"Getting wiki detail failure. URISyntaxException: ${e.getMessage} Title: $pageTitle")
        None
      case e: Throwable =>
        throw e
    }
  }
}

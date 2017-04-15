package com.nulabinc.r2b.redmine.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
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
        logger.error(e.getMessage, e)
        Seq.empty[WikiPage]
    }

  override def wikiDetail(pageTitle: String): WikiPageDetail =
    redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(apiConfig.projectKey, pageTitle)

}

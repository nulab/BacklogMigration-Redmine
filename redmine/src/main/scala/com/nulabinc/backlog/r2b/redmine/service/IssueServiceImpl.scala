package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.RedmineProjectId
import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.{Include, RedmineManager}
import spray.json.{JsNumber, JsonParser}

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class IssueServiceImpl @Inject() (
    apiConfig: RedmineApiConfiguration,
    projectId: RedmineProjectId,
    redmine: RedmineManager
) extends IssueService
    with Logging {

  override def countIssues(): Int = {
    val url =
      s"${apiConfig.url}/issues.json?limit=1&subproject_id=!*&project_id=${projectId.value}&key=${apiConfig.key}&status_id=*"
    val string = scala.io.Source.fromURL(url, "UTF-8").mkString
    JsonParser(string).asJsObject.getFields("total_count") match {
      case Seq(JsNumber(totalCount)) => totalCount.intValue
      case _                         => 0
    }
  }

  override def allIssues(params: Map[String, String]): Seq[Issue] =
    redmine.getIssueManager.getIssues(params.asJava).asScala.toSeq

  override def issueOfId(id: Integer, include: Include*): Issue = {
    logger.debug("Get an issue ID: " + id)
    redmine.getIssueManager.getIssueById(id, include: _*)
  }

  override def tryIssueOfId(
      id: Integer,
      include: Include*
  ): Either[Throwable, Issue] =
    try {
      Right(redmine.getIssueManager.getIssueById(id, include: _*))
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Left(e)
    }

}

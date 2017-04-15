package com.nulabinc.r2b.redmine.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.taskadapter.redmineapi.bean.Issue
import com.taskadapter.redmineapi.{Include, RedmineManager}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import spray.json.{JsNumber, JsonParser}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueServiceImpl @Inject()(apiConfig: RedmineConfig, @Named("projectId") projectId: Int, redmine: RedmineManager)
    extends IssueService
    with Logging {

  override def countIssues(): Int = {
    val string = scala.io.Source
      .fromURL(s"${apiConfig.url}/issues.json?limit=1&subproject_id=!*&project_id=${projectId}&key=${apiConfig.key}&status_id=*")
      .mkString
    JsonParser(string).asJsObject.getFields("total_count") match {
      case Seq(JsNumber(totalCount)) => totalCount.intValue()
      case _                         => 0
    }
  }

  override def allIssues(params: Map[String, String]): Seq[Issue] =
    redmine.getIssueManager.getIssues(params.asJava).asScala

  override def issueOfId(id: Integer, include: Include*): Issue =
    redmine.getIssueManager.getIssueById(id, include: _*)

  override def tryIssueOfId(id: Integer, include: Include*): Either[Throwable, Issue] =
    try {
      Right(redmine.getIssueManager.getIssueById(id, include: _*))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Left(e)
    }

  private[this] def httpGet(url: String): String = {
    val httpGet: HttpGet = new HttpGet(url)
    val client           = new DefaultHttpClient
    val response         = client.execute(httpGet)
    EntityUtils.toString(response.getEntity, "UTF-8")
  }

}

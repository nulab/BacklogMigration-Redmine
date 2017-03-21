package com.nulabinc.r2b.service

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.RedmineConfig
import com.nulabinc.r2b.domain._
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import spray.json.JsonParser

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class RedmineService(redmineConfig: RedmineConfig) extends Logging {

  import RedmineJsonProtocol._

  val redmine: RedmineManager = RedmineManagerFactory.createWithApiKey(redmineConfig.url, redmineConfig.key)

  def getIssuesCount(projectId: Int): Int = {
    val url = redmineConfig.url + "/issues.json?limit=1&subproject_id=!*&project_id=" + projectId + "&key=" + redmineConfig.key + "&status_id=*"
    val str: String = httpGet(url)
    val redmineIssuesWrapper: RedmineIssuesWrapper = JsonParser(str).convertTo[RedmineIssuesWrapper]
    redmineIssuesWrapper.total_count
  }

  private[this] def httpGet(url: String): String = {
    val httpGet: HttpGet = new HttpGet(url)
    val client = new DefaultHttpClient
    val response = client.execute(httpGet)
    EntityUtils.toString(response.getEntity, "UTF-8")
  }

  def getIssues(params: Map[String, String]): Seq[Issue] = {
    redmine.getIssueManager.getIssues(params.asJava).asScala
  }

  def getIssueById(id: Integer, include: Include*): Issue = {
    redmine.getIssueManager.getIssueById(id, include: _*)
  }

  def getCustomFieldDefinitions(): Either[Throwable, Seq[CustomFieldDefinition]] = {
    try {
      Right(redmine.getCustomFieldManager.getCustomFieldDefinitions.asScala)
    } catch {
      case e: Throwable => Left(e)
    }
  }

  def optProject(projectKey: String): Option[Project] =
    try {
      Some(redmine.getProjectManager.getProjectByKey(projectKey))
    } catch {
      case e: NotFoundException =>
        log.error(e)
        None
    }

  def getMemberships(projectKey: String): Seq[Membership] = {
    try {
      redmine.getMembershipManager.getMemberships(projectKey).asScala
    } catch {
      case e: NotFoundException =>
        log.error(e)
        Seq.empty[Membership]
    }
  }

  def getCategories(projectId: Int): Seq[IssueCategory] = {
    try {
      redmine.getIssueManager.getCategories(projectId).asScala
    } catch {
      case e: NotFoundException =>
        log.error(e)
        Seq.empty[IssueCategory]
    }
  }

  def getTrackers(): Seq[Tracker] = {
    try {
      redmine.getIssueManager.getTrackers.asScala
    } catch {
      case e: NotFoundException =>
        log.error(e)
        Seq.empty[Tracker]
    }
  }

  def getWikiPagesByProject(projectKey: String): Seq[WikiPage] = {
    try {
      redmine.getWikiManager.getWikiPagesByProject(projectKey).asScala
    } catch {
      case e: Exception =>
        log.error(e)
        Seq.empty[WikiPage]
    }
  }

  def getWikiPageDetailByProjectAndTitle(projectKey: String, pageTitle: String): WikiPageDetail = {
    redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(projectKey, pageTitle)
  }

  def getUsers(): Seq[User] = {
    val users: Seq[User] = redmine.getUserManager.getUsers.asScala
    users.flatMap(user => getUserById(user.getId))
  }

  def getUserById(id: Int): Option[User] = {
    try {
      Some(redmine.getUserManager.getUserById(id))
    } catch {
      case e: NotFoundException =>
        log.error(e)
        None
    }
  }

  def getNews(projectKey: String): Seq[News] = {
    try {
      redmine.getProjectManager.getNews(projectKey).asScala
    } catch {
      case e: NotAuthorizedException =>
        log.error(e)
        Seq.empty[News]
    }
  }

  def getGroupById(id: Int): Group = {
    redmine.getUserManager.getGroupById(id)
  }

  def getStatuses(): Seq[IssueStatus] = {
    try {
      redmine.getIssueManager.getStatuses.asScala
    } catch {
      case e: NotFoundException =>
        log.error(e)
        Seq.empty[IssueStatus]
    }
  }

  def getIssuePriorities(): Seq[IssuePriority] = {
    try {
      redmine.getIssueManager.getIssuePriorities.asScala
    } catch {
      case nfe: NotFoundException =>
        log.error(nfe)
        Seq.empty[IssuePriority]
      case rfe: RedmineFormatException =>
        log.error(rfe)
        Seq.empty[IssuePriority]
    }
  }

  def getVersions(projectID: Int): Seq[Version] = {
    try {
      redmine.getProjectManager.getVersions(projectID).asScala
    } catch {
      case rae: RedmineAuthenticationException =>
        log.error(rae)
        Seq.empty[Version]
      case nfe: NotFoundException =>
        log.error(nfe)
        Seq.empty[Version]
    }
  }

}
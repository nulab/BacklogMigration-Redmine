package com.nulabinc.r2b.service

import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain.{RedmineJsonProtocol, RedmineIssuesWrapper}
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsonParser

import scala.collection.JavaConversions._

/**
  * @author uchida
  */
class RedmineService(r2bConf: R2BConfig) {

  import RedmineJsonProtocol._

  private val log: Logger = LoggerFactory.getLogger("RedmineService")

  val redmine: RedmineManager = RedmineManagerFactory.createWithApiKey(r2bConf.redmineUrl, r2bConf.redmineKey)

  def getIssuesCount(projectId: Int): Int = {
    val url = r2bConf.redmineUrl + "/issues.json?limit=1&project_id=" + projectId + "&key=" + r2bConf.redmineKey + "&status_id=*"
    val str: String = httpGet(url)
    val redmineIssuesWrapper: RedmineIssuesWrapper = JsonParser(str).convertTo[RedmineIssuesWrapper]
    redmineIssuesWrapper.total_count
  }

  private def httpGet(url: String): String = {
    val httpGet: HttpGet = new HttpGet(url)
    val client = new DefaultHttpClient
    val response = client.execute(httpGet)
    EntityUtils.toString(response.getEntity, "UTF-8")
  }

  def getIssues(params: Map[String, String]): Seq[Issue] = {
    redmine.getIssueManager.getIssues(params)
  }

  def getIssueById(id: Integer, include: Include*): Issue = {
    redmine.getIssueManager.getIssueById(id, include: _*)
  }

  def getCustomFieldDefinitions: Either[Throwable, Seq[CustomFieldDefinition]] = {
    try {
      Right(redmine.getCustomFieldManager.getCustomFieldDefinitions)
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getProjects: Seq[Project] = {
    val projects: Seq[Either[Throwable, Project]] = r2bConf.projects.map(getProject)
    projects.filter(_.isRight).map(_.right.get)
  }

  def getProject(projectKey: ParamProjectKey): Either[Throwable, Project] =
    try {
      Right(redmine.getProjectManager.getProjectByKey(projectKey.redmine))
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }

  def getMemberships(projectKey: String): Either[Throwable, Seq[Membership]] = {
    try {
      Right(redmine.getMembershipManager.getMemberships(projectKey))
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getCategories(projectId: Int): Either[Throwable, Seq[IssueCategory]] = {
    try {
      Right(redmine.getIssueManager.getCategories(projectId))
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getTrackers: Either[Throwable, Seq[Tracker]] = {
    try {
      Right(redmine.getIssueManager.getTrackers)
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getWikiPagesByProject(projectKey: String): Either[Throwable, Seq[WikiPage]] = {
    try {
      Right(redmine.getWikiManager.getWikiPagesByProject(projectKey))
    } catch {
      case e: RedmineAuthenticationException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getWikiPageDetailByProjectAndTitle(projectKey: String, pageTitle: String): WikiPageDetail = {
    redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(projectKey, pageTitle)
  }

  def getUsers: Seq[User] = {
    val users: Seq[User] = redmine.getUserManager.getUsers
    users.map(user => getUserById(user.getId))
  }

  def getUserById(id: Int): User = {
    redmine.getUserManager.getUserById(id)
  }

  def getNews(projectKey: String): Seq[News] = {
    redmine.getProjectManager.getNews(projectKey)
  }

  def getGroups: Either[Throwable, Seq[Group]] = {
    try {
      Right(redmine.getUserManager.getGroups)
    } catch {
      case e: RedmineAuthenticationException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getGroupById(id: Int): Group = {
    redmine.getUserManager.getGroupById(id)
  }

  def getStatuses(): Seq[IssueStatus] = {
    try {
      redmine.getIssueManager.getStatuses
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Seq.empty[IssueStatus]
    }
  }

  def getIssuePriorities: Either[Throwable, Seq[IssuePriority]] = {
    try {
      Right(redmine.getIssueManager.getIssuePriorities)
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getVersions(projectID: Int): Either[Throwable, Seq[Version]] = {
    try {
      Right(redmine.getProjectManager.getVersions(projectID))
    } catch {
      case e: RedmineAuthenticationException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def getMemberships(projectID: Int): Either[Throwable, Seq[Membership]] = {
    try {
      Right(redmine.getMembershipManager.getMemberships(projectID))
    } catch {
      case e: NotFoundException =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

}
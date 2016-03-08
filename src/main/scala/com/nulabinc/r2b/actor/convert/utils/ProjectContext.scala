package com.nulabinc.r2b.actor.convert.utils

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.Issue.StatusType
import com.nulabinc.backlog4j.Status
import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineProject}
import com.nulabinc.r2b.service._
import com.taskadapter.redmineapi.bean.IssueStatus

/**
  * @author uchida
  */
case class ProjectContext(conf: R2BConfig, project: RedmineProject) {

  val redmineProjectKey = project.identifier
  val backlogProjectKey = convertProjectKey(project.identifier, conf.projects)

  val userMapping: ConvertUserMapping = new ConvertUserMapping()
  val statusMapping: ConvertStatusMapping = new ConvertStatusMapping()
  val priorityMapping: ConvertPriorityMapping = new ConvertPriorityMapping()

  val redmineService: RedmineService = new RedmineService(conf)
  val backlogService: BacklogService = new BacklogService(BacklogConfig(conf.backlogUrl, conf.backlogKey))
  val backlogStatuses: Seq[Status] = backlogService.getStatuses

  private val customFieldConverter = new CustomFieldConverter(conf)
  val customFieldDefinitions: Seq[RedmineCustomFieldDefinition] = customFieldConverter.execute(redmineService.getCustomFieldDefinitions)

  val users = redmineService.getUsers()

  val statuses = redmineService.getStatuses()

  val priorities = redmineService.getIssuePriorities()

  val trackers = redmineService.getTrackers()

  val categories = redmineService.getCategories(project.id)

  val versions = redmineService.getVersions(project.id)

  val memberships = redmineService.getMemberships(project.identifier)

  val projects = redmineService.getProjects()

  def getCustomFieldDefinitionsName(strId: String): String = {
    val id: Int = strId.toInt
    customFieldDefinitions.find(_.id == id).map(_.name).getOrElse("")
  }

  def getUserLoginId(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      users.find(_.getId == id).map(_.getLogin)
    }
    result.flatten
  }

  def getUserLoginId(id: Int): Option[String] = users.find(_.getId == id).map(_.getLogin)

  def getUserFullname(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      users.find(_.getId == id).map(_.getFullName)
    }
    result.flatten
  }

  def getStatusName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      statuses.find(_.getId == id).map(_.getName)
    }
    result.flatten.map(statusMapping.convert)
  }

  def getDefaultStatusName(): Option[String] = {
    backlogStatuses.find(_.getStatus == StatusType.Open).map(_.getName)
  }

  def getPriorityName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      priorities.find(_.getId == id).map(_.getName)
    }
    result.flatten.map(priorityMapping.convert)
  }

  def getTrackerName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      trackers.find(_.getId == id).map(_.getName)
    }
    result.flatten
  }

  def getCategoryName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      categories.find(_.getId == id).map(_.getName)
    }
    result.flatten
  }

  def getVersionName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      versions.find(_.getId == id).map(_.getName)
    }
    result.flatten
  }

  def getVersions() = versions.map(_.getName)

  def getMembershipUserName(id: Option[String]): Option[String] = {
    val result = for {strId <- id} yield {
      val id: Int = strId.toInt
      memberships.filter(_.getUser != null).find(_.getUser.getId == id).map(_.getUser.getFullName)
    }
    result.flatten
  }

  def getMemberships() = memberships.filter(_.getUser != null).map(_.getUser.getFullName)

  private def convertProjectKey(redmineIdentifier: String, paramProjectKeys: Seq[ParamProjectKey]): String = {
    val paramProjectKey: Option[ParamProjectKey] = paramProjectKeys.find(projectKey => projectKey.redmine == redmineIdentifier)
    paramProjectKey match {
      case Some(projectKey) => projectKey.getBacklogKey()
      case None => redmineIdentifier.toUpperCase.replaceAll("-", "_")
    }
  }

  def getProjectName(id: Int): Option[String] = projects.find(_.getId == id).map(_.getName)

}

package com.nulabinc.backlog.r2b.redmine.modules

import com.google.inject.AbstractModule
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineProjectId}
import com.nulabinc.backlog.r2b.redmine.service._
import com.taskadapter.redmineapi.bean._
import com.taskadapter.redmineapi.{RedmineFormatException, RedmineManager, RedmineManagerFactory}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class RedmineDefaultModule(apiConfig: RedmineApiConfiguration) extends AbstractModule with Logging {

  override def configure() = {

    //base
    val redmine = createRedmineClient()
    val project = redmine.getProjectManager.getProjectByKey(apiConfig.projectKey)
    bind(classOf[RedmineManager]).toInstance(redmine)
    bind(classOf[Project]).toInstance(project)
    bind(classOf[RedmineApiConfiguration]).toInstance(apiConfig)
    bind(classOf[PropertyValue]).toInstance(createPropertyValue(redmine, project))
    bind(classOf[RedmineProjectId]).toInstance(RedmineProjectId(project.getId))

    //service
    bind(classOf[IssueService]).to(classOf[IssueServiceImpl])
    bind(classOf[MembershipService]).to(classOf[MembershipServiceImpl])
    bind(classOf[ProjectService]).to(classOf[ProjectServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[WikiService]).to(classOf[WikiServiceImpl])
    bind(classOf[CustomFieldService]).to(classOf[CustomFieldServiceImpl])
    bind(classOf[IssueCategoryService]).to(classOf[IssueCategoryServiceImpl])
    bind(classOf[IssuePriorityService]).to(classOf[IssuePriorityServiceImpl])
    bind(classOf[NewsService]).to(classOf[NewsServiceImpl])
    bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
    bind(classOf[TrackerService]).to(classOf[TrackerServiceImpl])
    bind(classOf[VersionService]).to(classOf[VersionServiceImpl])
    bind(classOf[PriorityService]).to(classOf[PriorityServiceImpl])
  }

  private[this] def createRedmineClient(): RedmineManager = {
    val transportConfig = RedmineManagerFactory.createShortTermConfig(RedmineManagerFactory.createInsecureConnectionManager())
    RedmineManagerFactory.createWithApiKey(apiConfig.url, apiConfig.key, transportConfig)
  }

  private[this] def createPropertyValue(redmine: RedmineManager, project: Project): PropertyValue = {
    val versions = try {
      redmine.getProjectManager.getVersions(project.getId).asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[Version]
    }
    val categories = try {
      redmine.getIssueManager.getCategories(project.getId).asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssueCategory]
    }
    val priorities = try {
      redmine.getIssueManager.getIssuePriorities.asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssuePriority]
    }
    val trackers = try {
      redmine.getIssueManager.getTrackers.asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[Tracker]
    }
    val memberships = try {
      redmine.getMembershipManager.getMemberships(apiConfig.projectKey).asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[Membership]
    }
    val statuses = try {
      redmine.getIssueManager.getStatuses.asScala
    } catch {
      case e: Exception =>
        logger.warn(e.getMessage, e)
        Seq.empty[IssueStatus]
    }
    val users = redmine.getUserManager.getUsers.asScala

    val customFieldServiceImpl = new CustomFieldServiceImpl(apiConfig, redmine)
    val customFieldDefinitions = customFieldServiceImpl.allCustomFieldDefinitions()

    PropertyValue(users, versions, categories, priorities, trackers, memberships, statuses, customFieldDefinitions)
  }

}

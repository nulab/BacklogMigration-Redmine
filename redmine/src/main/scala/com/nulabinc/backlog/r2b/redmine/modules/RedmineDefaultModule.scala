package com.nulabinc.backlog.r2b.redmine.modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.service._
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue
import com.nulabinc.backlog.r2b.redmine.service._
import com.taskadapter.redmineapi.bean.Project
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class RedmineDefaultModule(apiConfig: RedmineApiConfiguration) extends AbstractModule {

  override def configure() = {

    //base
    val redmine = createRedmineClient()
    val project = redmine.getProjectManager.getProjectByKey(apiConfig.projectKey)
    bind(classOf[RedmineManager]).toInstance(redmine)
    bind(classOf[Project]).toInstance(project)
    bind(classOf[RedmineApiConfiguration]).toInstance(apiConfig)
    bind(classOf[PropertyValue]).toInstance(createPropertyValue(redmine, project))
    bind(classOf[Int]).annotatedWith(Names.named("projectId")).toInstance(project.getId)

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
    val versions    = redmine.getProjectManager.getVersions(project.getId).asScala
    val categories  = redmine.getIssueManager.getCategories(project.getId).asScala
    val users       = redmine.getUserManager.getUsers.asScala
    val priorities  = redmine.getIssueManager.getIssuePriorities.asScala
    val trackers    = redmine.getIssueManager.getTrackers.asScala
    val memberships = redmine.getMembershipManager.getMemberships(apiConfig.projectKey).asScala
    val statuses    = redmine.getIssueManager.getStatuses.asScala

    val customFieldServiceImpl = new CustomFieldServiceImpl(apiConfig, redmine)
    val customFieldDefinitions = customFieldServiceImpl.allCustomFieldDefinitions()

    PropertyValue(users, versions, categories, priorities, trackers, memberships, statuses, customFieldDefinitions)
  }

}

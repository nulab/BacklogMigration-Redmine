package com.nulabinc.r2b.di

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.nulabinc.backlog.migration.conf.BacklogDirectory
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory}
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.r2b.actor.redmine.ExportInfo
import com.nulabinc.r2b.conf.{AppConfiguration, RedmineDirectory}
import com.nulabinc.r2b.service._
import com.nulabinc.r2b.service.convert._
import com.taskadapter.redmineapi.bean.{Project, User}
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}

/**
  * @author uchida
  */
class RedmineModule(config: AppConfiguration, needUsers: Seq[User]) extends AbstractModule {

  override def configure() = {

    //base
    val redmine = createRedmineClient()
    val backlog = createBacklogClient()
    val project = redmine.getProjectManager.getProjectByKey(config.projectKeyMap.redmine)
    bind(classOf[AppConfiguration]).toInstance(config)
    bind(classOf[RedmineManager]).toInstance(redmine)
    bind(classOf[Project]).toInstance(project)
    bind(classOf[ExportInfo]).toInstance(ExportInfo(needUsers))
    bind(classOf[String])
      .annotatedWith(Names.named("projectKey"))
      .toInstance(config.projectKeyMap.redmine)
    bind(classOf[Int])
      .annotatedWith(Names.named("projectId"))
      .toInstance(project.getId)
    bind(classOf[String])
      .annotatedWith(Names.named("url"))
      .toInstance(config.redmineConfig.url)
    bind(classOf[String])
      .annotatedWith(Names.named("key"))
      .toInstance(config.redmineConfig.key)

    //directory
    bind(classOf[RedmineDirectory]).toInstance(new RedmineDirectory(config.projectKeyMap.redmine))
    bind(classOf[BacklogDirectory]).toInstance(new BacklogDirectory(config.projectKeyMap.getBacklogKey()))

    //mapping
    val userMapping = new ConvertUserMapping()
    val statusMapping = new ConvertStatusMapping()
    val priorityMapping = new ConvertPriorityMapping()
    bind(classOf[ConvertUserMapping]).toInstance(userMapping)
    bind(classOf[ConvertStatusMapping]).toInstance(statusMapping)
    bind(classOf[ConvertPriorityMapping]).toInstance(priorityMapping)

    //service
    val propertyServiceImpl = new PropertyServiceImpl(project.getId, redmine, backlog, statusMapping, priorityMapping)
    bind(classOf[PropertyService]).toInstance(propertyServiceImpl)
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
    bind(classOf[AttachmentDownloadService]).to(classOf[AttachmentDownloadServiceImpl])

    //convert
    bind(classOf[ConvertIssueService]).to(classOf[ConvertIssueServiceImpl])
    bind(classOf[ConvertCommentService]).to(classOf[ConvertCommentServiceImpl])
    bind(classOf[ConvertWikiService]).to(classOf[ConvertWikiServiceImpl])
    bind(classOf[ConvertCustomFieldDefinitionService]).to(classOf[ConvertCustomFieldDefinitionServiceImpl])
    bind(classOf[ConvertJournalDetailService]).to(classOf[ConvertJournalDetailServiceImpl])
  }

  private[this] def createRedmineClient(): RedmineManager =
    RedmineManagerFactory.createWithApiKey(config.redmineConfig.url, config.redmineConfig.key)

  private[this] def createBacklogClient(): BacklogClient = {
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(config.backlogConfig.url)
    val configure: BacklogConfigure = backlogPackageConfigure.apiKey(config.backlogConfig.key)
    new BacklogClientFactory(configure).newClient()
  }

}

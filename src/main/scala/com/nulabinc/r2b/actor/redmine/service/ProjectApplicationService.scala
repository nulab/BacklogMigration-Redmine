package com.nulabinc.r2b.actor.redmine.service

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Injector
import com.nulabinc.backlog.migration.di.akkaguice.GuiceAkkaExtension
import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.actor.redmine.ContentActor
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.service._
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean._
import net.codingwell.scalaguice.InjectorExtensions._

import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
class ProjectApplicationService @Inject()(
                                           project: Project,
                                           userService: UserService,
                                           customFieldService: CustomFieldService,
                                           trackerService: TrackerService,
                                           statusService: StatusService,
                                           issuePriorityService: IssuePriorityService,
                                           membershipService: MembershipService,
                                           issueCategoryService: IssueCategoryService,
                                           versionService: VersionService,
                                           newsService: NewsService,
                                           redmineDirectory: RedmineDirectory) extends Logging {

  private val logKInd = LOG_Header2

  def execute(injector: Injector) = {
    val system = injector.instance[ActorSystem]
    val contentActor = system.actorOf(GuiceAkkaExtension(system).props(ContentActor.name))
    contentActor ! ContentActor.Do()
    system.awaitTermination(Duration.Inf)
    property()
  }

  private[this] def property() = {

    IOUtil.output(redmineDirectory.PROJECTS, RedmineMarshaller.Project(project))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_user_export")))
    IOUtil.output(redmineDirectory.USERS, RedmineMarshaller.Users(userService.allUsers()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_custom_fields_export")))
    IOUtil.output(redmineDirectory.CUSTOM_FIELDS, RedmineMarshaller.CustomFieldDefinition(customFieldService.allCustomFieldDefinitions()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_issue_trackers_export")))
    IOUtil.output(redmineDirectory.TRACKERS, RedmineMarshaller.Tracker(trackerService.allTrackers()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_issue_statuses_export")))
    IOUtil.output(redmineDirectory.ISSUE_STATUSES, RedmineMarshaller.IssueStatus(statusService.allStatuses()))

    IOUtil.output(redmineDirectory.PRIORITY, RedmineMarshaller.IssuePriority(issuePriorityService.allIssuePriorities()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_memberships_export")))
    val memberships: Seq[Membership] = membershipService.allMemberships()
    IOUtil.output(redmineDirectory.getMembershipsPath(), RedmineMarshaller.Membership(memberships))

    val groups: Seq[Group] = memberships.flatMap(membership => Option(membership.getGroup))
    IOUtil.output(redmineDirectory.GROUP_USERS, RedmineMarshaller.Group(groups))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_issue_categories_export")))
    IOUtil.output(redmineDirectory.getIssueCategoriesPath(), RedmineMarshaller.IssueCategory(issueCategoryService.allCategories()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_versions_export")))
    IOUtil.output(redmineDirectory.getVersionsPath(), RedmineMarshaller.Versions(versionService.allVersions()))

    log.info(showMessage(logKInd, Messages("export.execute_redmine_news_export")))
    IOUtil.output(redmineDirectory.getNewsPath(), RedmineMarshaller.News(newsService.allNews(), userService.allUsers()))

  }

}


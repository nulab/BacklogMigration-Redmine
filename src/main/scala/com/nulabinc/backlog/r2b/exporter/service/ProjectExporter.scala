package com.nulabinc.backlog.r2b.exporter.service

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.Convert
import com.nulabinc.backlog.migration.common.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, IOUtil, Logging, ProgressBar}
import com.nulabinc.backlog.r2b.exporter.actor.ContentActor
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.exporter.core.ExportContextProvider
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.redmine.service.{MembershipService, _}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean._
import spray.json._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
private[exporter] class ProjectExporter @Inject()(implicit val projectWrites: ProjectWrites,
                                                  implicit val customFieldDefinitionsWrites: CustomFieldDefinitionsWrites,
                                                  implicit val versionsWrites: VersionsWrites,
                                                  implicit val issueTypesWrites: IssueTypesWrites,
                                                  implicit val issueCategoriesWrites: IssueCategoriesWrites,
                                                  implicit val newsWrites: NewsWrites,
                                                  implicit val mappingUserWrites: MappingUserWrites,
                                                  membershipWrites: MembershipWrites,
                                                  groupsWrites: GroupsWrites,
                                                  backlogPaths: BacklogPaths,
                                                  project: Project,
                                                  customFieldService: CustomFieldService,
                                                  trackerService: TrackerService,
                                                  membershipService: MembershipService,
                                                  issueCategoryService: IssueCategoryService,
                                                  versionService: VersionService,
                                                  newsService: NewsService,
                                                  exportContextProvider: ExportContextProvider,
                                                  backlogTextFormattingRule: BacklogTextFormattingRule)
    extends Logging {

  def boot(mappingContainer: MappingContainer): Unit = {
    val exportContext = exportContextProvider.get()
    val system        = ActorSystem.apply("main-actor-system")
    val contentActor  = system.actorOf(Props(new ContentActor(exportContext, backlogTextFormattingRule)))
    contentActor ! ContentActor.Do

    Await.result(system.whenTerminated, Duration.Inf)

    property(mappingContainer)
  }

  private[this] def property(mappingContainer: MappingContainer): Unit = {
    val allMemberships: Seq[Membership] = membershipService.allMemberships()

    //project
    IOUtil.output(backlogPaths.projectJson, BacklogProjectWrapper(Convert.toBacklog(project)).toJson.prettyPrint)

    //group
    IOUtil.output(backlogPaths.groupsJson, BacklogGroupsWrapper(Convert.toBacklog(allMemberships)(groupsWrites)).toJson.prettyPrint)

    //memberships
    val allProjectUser = mutable.Set.empty[BacklogUser]
    val projectUsers   = Convert.toBacklog(allMemberships)(membershipWrites)
    projectUsers.foreach(projectUser => allProjectUser += projectUser)
    mappingContainer.user.map(mapping => Convert.toBacklog(mapping)).foreach(projectUser => allProjectUser += projectUser)
    IOUtil.output(backlogPaths.projectUsersJson, BacklogProjectUsersWrapper(allProjectUser.toSeq).toJson.prettyPrint)
    ConsoleOut.boldln(Messages("message.executed", Messages("common.project_user"), Messages("message.exported")), 1)

    //customFields
    val customFieldDefinitions = customFieldService.allCustomFieldDefinitions()
    IOUtil
      .output(backlogPaths.customFieldSettingsJson, BacklogCustomFieldSettingsWrapper(Convert.toBacklog(customFieldDefinitions)).toJson.prettyPrint)
    ConsoleOut.boldln(Messages("message.executed", Messages("common.custom_field"), Messages("message.exported")), 1)

    //versions
    val versions = versionService.allVersions()
    IOUtil.output(backlogPaths.versionsJson, BacklogVersionsWrapper(Convert.toBacklog(versions)).toJson.prettyPrint)
    ConsoleOut.boldln(Messages("message.executed", Messages("common.version"), Messages("message.exported")), 1)

    //trackers
    val trackers = trackerService.allTrackers()
    IOUtil.output(backlogPaths.issueTypesJson, BacklogIssueTypesWrapper(Convert.toBacklog(trackers)).toJson.prettyPrint)
    ConsoleOut.boldln(Messages("message.executed", Messages("common.trackers"), Messages("message.exported")), 1)

    //categories
    val categories = issueCategoryService.allCategories()
    IOUtil.output(backlogPaths.issueCategoriesJson, BacklogIssueCategoriesWrapper(Convert.toBacklog(categories)).toJson.prettyPrint)
    ConsoleOut.boldln(Messages("message.executed", Messages("common.category"), Messages("message.exported")), 1)

    //news
    val console = (ProgressBar.progress _)(Messages("common.news"), Messages("message.exporting"), Messages("message.exported"))
    val allNews = newsService.allNews()
    allNews.zipWithIndex.foreach {
      case (news, index) =>
        IOUtil.output(backlogPaths.wikiJson(news.getTitle), Convert.toBacklog(news).toJson.prettyPrint)
        console(index + 1, allNews.size)
    }

  }

}

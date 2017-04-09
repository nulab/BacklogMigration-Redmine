package com.nulabinc.r2b.exporter.service

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Injector
import com.nulabinc.backlog.migration.conf.BacklogPaths
import com.nulabinc.backlog.migration.converter.Convert
import com.nulabinc.backlog.migration.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.modules.akkaguice.GuiceAkkaExtension
import com.nulabinc.backlog.migration.utils.{ConsoleOut, IOUtil, Logging}
import com.nulabinc.r2b.exporter.actor.{ContentActor, ExportInfo}
import com.nulabinc.r2b.exporter.convert._
import com.nulabinc.r2b.redmine.service._
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean._
import net.codingwell.scalaguice.InjectorExtensions._
import spray.json._

import scala.concurrent.duration.Duration

/**
  * @author uchida
  */
class ProjectApplicationService @Inject()(implicit val projectWrites: ProjectWrites,
                                          implicit val customFieldDefinitionsWrites: CustomFieldDefinitionsWrites,
                                          implicit val versionsWrites: VersionsWrites,
                                          implicit val issueTypesWrites: IssueTypesWrites,
                                          implicit val issueCategoriesWrites: IssueCategoriesWrites,
                                          membershipWrites: MembershipWrites,
                                          groupsWrites: GroupsWrites,
                                          backlogPaths: BacklogPaths,
                                          project: Project,
                                          customFieldService: CustomFieldService,
                                          trackerService: TrackerService,
                                          membershipService: MembershipService,
                                          issueCategoryService: IssueCategoryService,
                                          versionService: VersionService)
    extends Logging {

  def execute(injector: Injector) = {
    val system = injector.instance[ActorSystem]

    val contentActor = system.actorOf(GuiceAkkaExtension(system).props(ContentActor.name))
    contentActor ! ContentActor.Do

    system.awaitTermination(Duration.Inf)
    property()
  }

  private[this] def property() = {
    //project
    IOUtil.output(backlogPaths.projectJson, BacklogProjectWrapper(Convert.toBacklog(project)).toJson.prettyPrint)

    val allMemberships: Seq[Membership] = membershipService.allMemberships()

    //group
    IOUtil.output(backlogPaths.groupsJson, BacklogGroupsWrapper(Convert.toBacklog(allMemberships)(groupsWrites)).toJson.prettyPrint)

    //memberships
    IOUtil.output(backlogPaths.projectUsersJson, BacklogProjectUsersWrapper(Convert.toBacklog(allMemberships)(membershipWrites)).toJson.prettyPrint)
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
    //TODO
//    IOUtil.output(redminePaths.newsJson, RedmineMarshaller.News(newsService.allNews(), userService.allUsers()))
//    ConsoleOut.boldln(Messages("message.executed", Messages("common.news"), Messages("message.exported")), 1)

  }

}

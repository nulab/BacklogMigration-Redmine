package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain._
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service._
import com.nulabinc.r2b.service.convert.{ConvertCustomFieldDefinitions, ConvertIssueTypes, ConvertProjects, ConvertVersions}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

/**
  * @author uchida
  */
class ProjectsActor(conf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  import BacklogJsonProtocol._

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  def receive: Receive = {
    case ProjectsActor.Do =>
      val result = for {redmineProjects <- RedmineUnmarshaller.projects()} yield {

        IOUtil.output(
          BacklogConfigBase.Backlog.PROJECTS,
          ConvertProjects(redmineProjects, conf.projects).toJson.prettyPrint)

        if (redmineProjects.nonEmpty) {
          info(Messages("message.start_projects_convert"))
          redmineProjects.foreach(startActors)
        } else context.stop(self)
      }
      if (result.isEmpty) context.stop(self)
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.stop(self)
  }

  private def startActors(redmineProject: RedmineProject) = {

    val pctx = new ProjectContext(conf, redmineProject)

    projectUsers(pctx)
    issueCategories(pctx)
    customFields(pctx)
    issueTypes(pctx)
    versions(pctx)

    start(Props(new NewsActor(pctx)), NewsActor.actorName) ! NewsActor.Do
    start(Props(new WikisActor(pctx)), WikisActor.actorName) ! WikisActor.Do
    start(Props(new IssuesActor(pctx)), IssuesActor.actorName) ! IssuesActor.Do
  }

  private def projectUsers(pctx: ProjectContext) = {
    for {membershipsUsers <- RedmineUnmarshaller.membershipsUsers(pctx.redmineProjectKey)} yield {

      info(Messages("message.execute_project_users_convert", pctx.project.name))

      val userIds: Seq[String] = membershipsUsers.flatMap(membershipsUser => pctx.getUserLoginId(membershipsUser.id))
      IOUtil.output(
        BacklogConfigBase.Backlog.getProjectUsersDir(pctx.backlogProjectKey),
        BacklogProjectUsersWrapper(userIds.map(pctx.userMapping.convert)).toJson.prettyPrint)
    }
  }

  private def issueCategories(pctx: ProjectContext) = {
    for {redmineIssueCategories <- RedmineUnmarshaller.categories(pctx.redmineProjectKey)} yield {

      info(Messages("message.execute_issue_categories_convert", pctx.project.name))

      IOUtil.output(
        BacklogConfigBase.Backlog.getIssueCategoriesDir(pctx.backlogProjectKey),
        BacklogIssueCategoriesWrapper(redmineIssueCategories.map(_.name)).toJson.prettyPrint)
    }
  }

  private def customFields(pctx: ProjectContext) = {
    for {customFieldDefinitions <- RedmineUnmarshaller.customFieldDefinitions()} yield {

      info(Messages("message.execute_custom_fields_convert", pctx.redmineProjectKey))

      val convertCustomFieldDefinitions = new ConvertCustomFieldDefinitions(pctx)
      val backlogCustomFieldDefinitionsWrapper = convertCustomFieldDefinitions.execute(customFieldDefinitions)
      IOUtil.output(
        BacklogConfigBase.Backlog.getCustomFieldsPath(pctx.backlogProjectKey),
        backlogCustomFieldDefinitionsWrapper.toJson.prettyPrint)
    }
  }

  private def issueTypes(pctx: ProjectContext) = {
    for {trackers <- RedmineUnmarshaller.trackers()} yield {

      info(Messages("message.execute_issue_types_convert", pctx.redmineProjectKey))

      IOUtil.output(
        BacklogConfigBase.Backlog.getIssueTypesDir(pctx.backlogProjectKey),
        ConvertIssueTypes(trackers).toJson.prettyPrint)
    }
  }

  private def versions(pctx: ProjectContext) = {
    for {redmineVersions <- RedmineUnmarshaller.versions(pctx.redmineProjectKey)} yield {

      info(Messages("message.execute_versions_convert", pctx.project.name))

      IOUtil.output(
        BacklogConfigBase.Backlog.getVersionsDir(pctx.backlogProjectKey),
        ConvertVersions(redmineVersions).toJson.prettyPrint)
    }
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
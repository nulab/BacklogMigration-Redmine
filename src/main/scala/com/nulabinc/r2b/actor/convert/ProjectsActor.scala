package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, AllForOneStrategy, Props, Terminated}
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain._
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service._
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

/**
 * @author uchida
 */
class ProjectsActor(r2bConf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  import BacklogJsonProtocol._

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  def receive: Receive = {
    case ProjectsActor.Do =>
      val result = for {redmineProjects <- RedmineUnmarshaller.projects()} yield {
        val backlogProjectsWrapper: BacklogProjectsWrapper = ConvertService.Projects(redmineProjects, r2bConf.projects)
        IOUtil.output(BacklogConfigBase.Backlog.PROJECTS, backlogProjectsWrapper.toJson.prettyPrint)
        if (redmineProjects.nonEmpty) {
          printlog(Messages("message.start_projects_convert"))
          redmineProjects.foreach(startActors)
        } else context.stop(self)
      }
      if (result.isEmpty) context.stop(self)
    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) context.stop(self)
  }

  private def startActors(redmineProject: RedmineProject) = {
    val projectInfo: ProjectInfo = ProjectInfo(redmineProject.name, ProjectKey(redmineProject.identifier, convertProjectKey(redmineProject.identifier, r2bConf.projects)))

    projectUsers(projectInfo)

    issueCategories(projectInfo)

    customFields(projectInfo)

    issueTypes(projectInfo)

    versions(projectInfo)

    start(Props(new NewsActor(r2bConf, projectInfo)), NewsActor.actorName) ! NewsActor.Do

    start(Props(new WikisActor(r2bConf, projectInfo)), WikisActor.actorName) ! WikisActor.Do

    start(Props(new IssuesActor(r2bConf, projectInfo)), IssuesActor.actorName) ! IssuesActor.Do
  }

  private def convertProjectKey(redmineIdentifier: String, paramProjectKeys: Seq[ParamProjectKey]): String = {
    val paramProjectKey: Option[ParamProjectKey] = paramProjectKeys.find(projectKey => projectKey.redmine == redmineIdentifier)
    paramProjectKey match {
      case Some(projectKey) => projectKey.getBacklogKey()
      case None => redmineIdentifier.toUpperCase.replaceAll("-", "_")
    }
  }

  private def projectUsers(projectInfo: ProjectInfo) = {
    for {redmineUsers <- RedmineUnmarshaller.users()
         membershipsUsers <- RedmineUnmarshaller.membershipsUsers(projectInfo.projectKey.redmine)} yield {

      printlog(Messages("message.execute_project_users_convert", projectInfo.name))


      val backlogProjectUsersWrapper: BacklogProjectUsersWrapper = ConvertService.Memberships(membershipsUsers, redmineUsers)
      IOUtil.output(BacklogConfigBase.Backlog.getProjectUsersDir(projectInfo.projectKey.backlog), backlogProjectUsersWrapper.toJson.prettyPrint)
    }
  }

  private def issueCategories(projectInfo: ProjectInfo) = {
    for {redmineIssueCategories <- RedmineUnmarshaller.categories(projectInfo.projectKey.redmine)} yield {

      printlog(Messages("message.execute_issue_categories_convert", projectInfo.name))

      val backlogIssueCategoriesWrapper: BacklogIssueCategoriesWrapper = ConvertService.IssueCategories(redmineIssueCategories)
      IOUtil.output(BacklogConfigBase.Backlog.getIssueCategoriesDir(projectInfo.projectKey.backlog), backlogIssueCategoriesWrapper.toJson.prettyPrint)
    }
  }

  private def customFields(projectInfo: ProjectInfo) = {
    for {customFieldDefinitions <- RedmineUnmarshaller.customFieldDefinitions()} yield {

      printlog(Messages("message.execute_custom_fields_convert", projectInfo.projectKey.redmine))

      val projectEnumerations: ProjectEnumerations = new ProjectEnumerations(projectInfo.projectKey.redmine)

      val backlogCustomFieldDefinitionsWrapper: BacklogCustomFieldDefinitionsWrapper = ConvertService.CustomFieldDefinitions(projectEnumerations, customFieldDefinitions)
      IOUtil.output(BacklogConfigBase.Backlog.getCustomFieldsPath(projectInfo.projectKey.backlog), backlogCustomFieldDefinitionsWrapper.toJson.prettyPrint)
    }
  }

  private def issueTypes(projectInfo: ProjectInfo) = {
    for {trackers <- RedmineUnmarshaller.trackers()} yield {

      printlog(Messages("message.execute_issue_types_convert", projectInfo.projectKey.redmine))

      val backlogIssueTypesWrapper: BacklogIssueTypesWrapper = ConvertService.IssueTypes(trackers)
      IOUtil.output(BacklogConfigBase.Backlog.getIssueTypesDir(projectInfo.projectKey.backlog), backlogIssueTypesWrapper.toJson.prettyPrint)
    }
  }

  private def versions(projectInfo: ProjectInfo) = {
    for {redmineVersions <- RedmineUnmarshaller.versions(projectInfo.projectKey.redmine)} yield {

      printlog(Messages("message.execute_versions_convert", projectInfo.name))

      val backlogVersionsWrapper: BacklogVersionsWrapper = ConvertService.Versions(redmineVersions)
      IOUtil.output(BacklogConfigBase.Backlog.getVersionsDir(projectInfo.projectKey.backlog), backlogVersionsWrapper.toJson.prettyPrint)
    }
  }

}

object ProjectsActor {

  case class Do()

  def actorName = s"ProjectsActor_$randomUUID"

}
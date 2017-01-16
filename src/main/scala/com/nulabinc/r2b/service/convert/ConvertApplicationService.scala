package com.nulabinc.r2b.service.convert

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.conf.{BacklogDirectory, BacklogProperty}
import com.nulabinc.backlog.migration.domain.BacklogJsonProtocol._
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.conf.{AppConfiguration, RedmineDirectory}
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.{ConvertUserMapping, PropertyService, RedmineUnmarshaller}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.Project
import spray.json._

import scalax.file.Path

/**
  * @author uchida
  */
class ConvertApplicationService @Inject()(
                                           @Named("projectKey") projectKey: String,
                                           config: AppConfiguration,
                                           project: Project,
                                           propertyService: PropertyService,
                                           backlogDirectory: BacklogDirectory,
                                           redmineDirectory: RedmineDirectory,
                                           userMapping: ConvertUserMapping,
                                           convertIssueService: ConvertIssueService,
                                           convertWikiService: ConvertWikiService,
                                           convertCustomFieldDefinitionService: ConvertCustomFieldDefinitionService) extends Logging {
  def execute() = {
    try {
      for {redmineProject <- RedmineUnmarshaller.project(redmineDirectory)} yield {
        convert(redmineProject)
      }
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
        throw e
    }
  }

  private[this] def convert(redmineProject: RedmineProject) = {
    project(redmineProject)
    groups()
    projectUsers()
    issueCategories()
    customFields()
    issueTypes()
    versions()
    issues()
    wikis()
  }

  private[this] def project(redmineProject: RedmineProject) =
    IOUtil.output(
      backlogDirectory.PROJECT,
      BacklogProjectWrapper(BacklogProject(name = redmineProject.name, key = config.projectKeyMap.getBacklogKey())).toJson.prettyPrint)

  private[this] def groups() =
    for {redmineUsers <- RedmineUnmarshaller.users(redmineDirectory)
         redmineGroups <- RedmineUnmarshaller.groups(redmineDirectory)} yield {

      def getUserIdsByGroupId(groupName: String, redmineUsers: Seq[RedmineUser]): Seq[String] =
        redmineUsers.filter(redmineUser => redmineUser.groups.contains(groupName)).map(_.login)

      log.info(showMessage(LOG_Header2, Messages("convert.start_groups_convert")))
      val backlogGroups: Seq[BacklogGroup] =
        redmineGroups.map(redmineGroup =>
          BacklogGroup(redmineGroup.name, getUserIdsByGroupId(redmineGroup.name, redmineUsers).map(userMapping.convert)))

      IOUtil.output(
        backlogDirectory.GROUPS,
        BacklogGroupsWrapper(backlogGroups).toJson.prettyPrint)
    }

  private[this] def projectUsers() =
    for {membershipsUsers <- RedmineUnmarshaller.membershipsUsers(redmineDirectory)} yield {

      log.info(showMessage(LOG_Header2, Messages("convert.execute_project_users_convert")))

      val userIds: Seq[String] = membershipsUsers.flatMap(membershipsUser => propertyService.optUser(membershipsUser.id))
      IOUtil.output(
        backlogDirectory.getProjectUsersDir(),
        BacklogProjectUsersWrapper(userIds.map(userMapping.convert)).toJson.prettyPrint)
    }


  private[this] def issueCategories() = {
    for {redmineIssueCategories <- RedmineUnmarshaller.categories(redmineDirectory)} yield {

      log.info(showMessage(LOG_Header2, Messages("convert.execute_issue_categories_convert")))

      IOUtil.output(
        backlogDirectory.getIssueCategoriesDir(),
        BacklogIssueCategoriesWrapper(redmineIssueCategories.map(category => BacklogIssueCategory(category.name, false))).toJson.prettyPrint)
    }
  }

  private[this] def customFields() = {
    for {customFieldDefinitions <- RedmineUnmarshaller.customFieldDefinitions(redmineDirectory)} yield {

      log.info(showMessage(LOG_Header2, Messages("convert.execute_custom_fields_convert")))

      val backlogCustomFieldDefinitions: Seq[BacklogCustomFieldDefinition] =
        customFieldDefinitions.map(customFieldDefinition => convertCustomFieldDefinitionService.convert(customFieldDefinition))
      IOUtil.output(
        backlogDirectory.getCustomFieldsPath(),
        BacklogCustomFieldDefinitionsWrapper(backlogCustomFieldDefinitions).toJson.prettyPrint)
    }
  }

  private[this] def issueTypes() = {
    for {trackers <- RedmineUnmarshaller.trackers(redmineDirectory)} yield {

      log.info(showMessage(LOG_Header2, Messages("convert.execute_issue_types_convert")))

      val backlogIssueTypes: Seq[BacklogIssueType] =
        trackers.map(tracker => BacklogIssueType(name = tracker.name, color = BacklogProperty.ISSUE_TYPE_COLOR.getStrValue, deleted = false))

      IOUtil.output(
        backlogDirectory.getIssueTypesDir(),
        BacklogIssueTypesWrapper(backlogIssueTypes).toJson.prettyPrint)
    }
  }

  private[this] def versions() = {
    for {redmineVersions <- RedmineUnmarshaller.versions(redmineDirectory)} yield {

      log.info(showMessage(LOG_Header2, Messages("convert.execute_versions_convert")))

      val backlogVersions: Seq[BacklogVersion] =
        redmineVersions.map(version => BacklogVersion(
          name = version.name,
          description = version.description,
          startDate = None,
          releaseDueDate = version.dueDate,
          deleted = false))

      IOUtil.output(
        backlogDirectory.getVersionsDir(),
        BacklogVersionsWrapper(backlogVersions).toJson.prettyPrint)
    }
  }

  private[this] def issues() = {
    val paths: Seq[Path] = IOUtil.directoryPaths(redmineDirectory.getIssuesDir())
    paths.zipWithIndex.foreach { case (path, count) => issue(path, count, paths.size) }
  }

  private[this] def issue(path: Path, count: Int, size: Int) = {
    for {redmineIssue <- RedmineUnmarshaller.issue(path.path + "/" + redmineDirectory.ISSUE_FILE_NAME)} yield {
      val backlogIssue: BacklogIssue = convertIssueService.convert(redmineIssue)
      IOUtil.output(backlogDirectory.getIssuePath(redmineIssue.id), backlogIssue.toJson.prettyPrint)

      //Copy attachments
      val redmineAttachments: Seq[RedmineAttachment] = redmineIssue.attachments
      redmineAttachments.foreach(redmineAttachment => copy(redmineAttachment, redmineIssue))

      log.info(showMessage(LOG_List, Messages("convert.execute_issue_convert", count + 1, size)))
    }
  }

  private[this] def copy(attachment: RedmineAttachment, redmineIssue: RedmineIssue) = {
    val dir: String = redmineDirectory.getIssueAttachmentDir(redmineIssue.id, attachment.id)
    val redmineFilePath: String = dir + "/" + attachment.fileName
    val convertFilePath: String = backlogDirectory.getIssueAttachmentPath(redmineIssue.id, attachment.id, attachment.fileName)
    IOUtil.copy(redmineFilePath, convertFilePath)
  }

  private[this] def wikis() = {
    val paths: Seq[Path] = IOUtil.directoryPaths(redmineDirectory.getWikisDir())
    paths.zipWithIndex.foreach { case (path, count) => wiki(path, count, paths.size) }
  }

  private[this] def wiki(wikiPath: Path, count: Int, size: Int) = {
    for {redmineWikiPage <- RedmineUnmarshaller.wiki(wikiPath.path + "/" + redmineDirectory.WIKI_FILE_NAME)} yield {

      //Convert and output backlog wiki
      val backlogWiki: BacklogWiki = convertWikiService.convert(redmineWikiPage)
      IOUtil.output(backlogDirectory.getWikiPath(redmineWikiPage.title), backlogWiki.toJson.prettyPrint)

      //Copy attachments
      val redmineAttachments: Seq[RedmineAttachment] = redmineWikiPage.attachments
      redmineAttachments.foreach(redmineAttachment => copy(redmineAttachment, redmineWikiPage))

      log.info(showMessage(LOG_List, Messages("convert.execute_wiki_convert", count + 1, size)))
    }
  }

  private[this] def copy(redmineAttachment: RedmineAttachment, redmineWikiPage: RedmineWikiPage) = {
    val dir: String = redmineDirectory.getWikiAttachmentDir(redmineWikiPage.title, redmineAttachment.id)
    val redmineFilePath: String = dir + "/" + redmineAttachment.fileName
    val convertFilePath: String = backlogDirectory.getWikiAttachmentPath(redmineWikiPage.title, redmineAttachment.id, redmineAttachment.fileName)
    IOUtil.copy(redmineFilePath, convertFilePath)
  }

}

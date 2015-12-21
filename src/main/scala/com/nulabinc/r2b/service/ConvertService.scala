package com.nulabinc.r2b.service

import java.util.Locale

import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain._
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.r2b.actor.convert.utils.ConvertComments
import com.nulabinc.r2b.actor.utils.IssueTag
import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain._
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
object ConvertService {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  private val userMapping: ConvertUserMapping = new ConvertUserMapping()
  private val statusMapping: ConvertStatusMapping = new ConvertStatusMapping()
  private val priorityMapping: ConvertPriorityMapping = new ConvertPriorityMapping()

  object CustomFieldDefinitions {
    def apply(projectEnumerations: ProjectEnumerations, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): BacklogCustomFieldDefinitionsWrapper =
      BacklogCustomFieldDefinitionsWrapper(redmineCustomFieldDefinitions.map(redmineCustomFieldDefinition => getBacklogCustomFieldDefinition(projectEnumerations, redmineCustomFieldDefinition)))

    private def getBacklogCustomFieldDefinition(projectEnumerations: ProjectEnumerations, customFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDefinition =
      BacklogCustomFieldDefinition(
        id = customFieldDefinition.id,
        name = customFieldDefinition.name,
        description = "",
        typeId = getTypeId(customFieldDefinition),
        required = customFieldDefinition.isRequired,
        applicableIssueTypes = customFieldDefinition.trackers.map(_.name),
        items = getPossibleValues(projectEnumerations, customFieldDefinition),
        initialValueNumeric = getInitialValueNumeric(customFieldDefinition),
        minNumeric = getMinNumeric(customFieldDefinition.maxLength),
        maxNumeric = getMaxNumeric(customFieldDefinition.maxLength),
        initialValueDate = getInitialValueDate(customFieldDefinition),
        minDate = None,
        maxDate = None)

    private def getPossibleValues(projectEnumerations: ProjectEnumerations, redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Seq[String] =
      redmineCustomFieldDefinition.fieldFormat match {
        case ConfigBase.FieldFormat.VERSION => projectEnumerations.Version.values()
        case ConfigBase.FieldFormat.USER => projectEnumerations.Membership.values()
        case ConfigBase.FieldFormat.BOOL => booleanPossibleValues()
        case _ => redmineCustomFieldDefinition.possibleValues
      }

    private def getInitialValueNumeric(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[Float] =
      if (redmineCustomFieldDefinition.fieldFormat == "int" || redmineCustomFieldDefinition.fieldFormat == "float")
        redmineCustomFieldDefinition.defaultValue.map(_.toFloat)
      else None

    private def getMinNumeric(value: Option[Int]): Option[Float] =
      value.map(value => math.pow(10, (value - 1) * (-1)).toFloat)

    private def getMaxNumeric(value: Option[Int]): Option[Float] =
      value.map(value => math.pow(10, value).toFloat)

    private def getInitialValueDate(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[String] =
      if (redmineCustomFieldDefinition.fieldFormat == ConfigBase.FieldFormat.DATE) redmineCustomFieldDefinition.defaultValue
      else None

    private def booleanPossibleValues(): Seq[String] = Seq(Messages("label.no"), Messages("label.yes"))

    private def getTypeId(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Int = redmineCustomFieldDefinition.fieldFormat match {
      case ConfigBase.FieldFormat.STRING | ConfigBase.FieldFormat.LINK => FieldType.Text.getIntValue
      case ConfigBase.FieldFormat.INT | ConfigBase.FieldFormat.FLOAT => FieldType.Numeric.getIntValue
      case ConfigBase.FieldFormat.DATE => FieldType.Date.getIntValue
      case ConfigBase.FieldFormat.TEXT => FieldType.TextArea.getIntValue
      case ConfigBase.FieldFormat.LIST => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
      case ConfigBase.FieldFormat.USER | ConfigBase.FieldFormat.VERSION => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
      case ConfigBase.FieldFormat.BOOL => FieldType.Radio.getIntValue
    }
  }

  object Issue {

    def apply(projectIdentifier: String, projectEnumerations: ProjectEnumerations, redmineIssue: RedmineIssue, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition], redmineUrl: String): BacklogIssue =
      BacklogIssue(
        id = redmineIssue.id,
        summary = redmineIssue.subject,
        parentIssueId = redmineIssue.parentIssueId.map(_.toLong),
        description = redmineIssue.description + "\n" + IssueTag.getTag(redmineIssue.id, redmineUrl),
        startDate = redmineIssue.startDate,
        dueDate = redmineIssue.dueDate,
        estimatedHours = redmineIssue.estimatedHours.map(_.toFloat),
        actualHours = redmineIssue.spentHours.map(_.toFloat),
        issueTypeName = redmineIssue.tracker,
        statusName = statusMapping.convert(redmineIssue.status),
        categoryName = redmineIssue.category,
        versionName = redmineIssue.version,
        priorityName = priorityMapping.convert(redmineIssue.priority),
        assigneeUserId = redmineIssue.assigneeId.map(userMapping.convert),
        attachments = getBacklogAttachments(redmineIssue.attachments),
        comments = ConvertComments(projectIdentifier, redmineIssue.id, projectEnumerations, userMapping, redmineIssue.journals),
        customFields = getBacklogCustomFields(projectEnumerations, redmineIssue.customFields, redmineCustomFieldDefinitions),
        createdUserId = redmineIssue.author.map(userMapping.convert),
        created = redmineIssue.createdOn,
        updatedUserId = redmineIssue.author.map(userMapping.convert),
        updated = redmineIssue.updatedOn)

    private def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
      attachments.map(getBacklogAttachment)

    private def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
      BacklogAttachment(attachment.id, attachment.fileName)

    private def getBacklogCustomFields(projectEnumerations: ProjectEnumerations, redmineCustomFields: Seq[RedmineCustomField], redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Seq[BacklogCustomField] =
      redmineCustomFields.map(redmineCustomField => getBacklogCustomField(projectEnumerations, redmineCustomField, redmineCustomFieldDefinitions))

    private def getBacklogCustomField(projectEnumerations: ProjectEnumerations, redmineCustomField: RedmineCustomField, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): BacklogCustomField =
      BacklogCustomField(
        id = redmineCustomField.id,
        name = redmineCustomField.name,
        value = getCustomFieldValue(projectEnumerations, redmineCustomField, redmineCustomFieldDefinitions),
        multiple = redmineCustomField.multiple,
        values = redmineCustomField.values)

    private def getCustomFieldValue(projectEnumerations: ProjectEnumerations, redmineCustomField: RedmineCustomField, redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Option[String] = {
      val redmineCustomFieldDefinition: RedmineCustomFieldDefinition = redmineCustomFieldDefinitions.find(_.id == redmineCustomField.id).get
      val fieldFormat: String = redmineCustomFieldDefinition.fieldFormat
      fieldFormat match {
        case ConfigBase.FieldFormat.VERSION => projectEnumerations.Version.convertValue(redmineCustomField.value)
        case ConfigBase.FieldFormat.USER => projectEnumerations.Membership.convertValue(redmineCustomField.value)
        case ConfigBase.FieldFormat.INT | ConfigBase.FieldFormat.FLOAT =>
          redmineCustomField.value.orElse(redmineCustomFieldDefinition.defaultValue)
        case _ => redmineCustomField.value
      }
    }

  }

  object Memberships {
    def apply(redmineMemberships: Seq[RedmineUser], redmineUsers: Seq[RedmineUser]): BacklogProjectUsersWrapper = {
      val userIds: Seq[String] = redmineMemberships
        .map(redmineMembership => redmineUsers.find(_.id == redmineMembership.id).map(_.login))
        .filter(_.isDefined).map(_.get)
      BacklogProjectUsersWrapper(userIds.map(userMapping.convert))
    }
  }

  object WikiPage {
    def apply(redmineWikiPage: RedmineWikiPage): BacklogWiki = {
      val sb = new StringBuilder
      if (redmineWikiPage.text.isDefined) sb.append(redmineWikiPage.text.get)
      if (redmineWikiPage.comments.isDefined) sb.append("\n\n\n").append(Messages("label.comment")).append(":").append(redmineWikiPage.comments.get)
      if (redmineWikiPage.parentTitle.isDefined) sb.append("\n").append(Messages("label.parent_page")).append(":[[").append(redmineWikiPage.parentTitle.get).append("]]")
      val content: Option[String] = if (sb.result().isEmpty) None else Some(sb.result())

      BacklogWiki(
        name = redmineWikiPage.title,
        content = content,
        createdUserId = redmineWikiPage.user.map(userMapping.convert),
        created = redmineWikiPage.createdOn,
        updatedUserId = redmineWikiPage.user.map(userMapping.convert),
        updated = redmineWikiPage.updatedOn,
        attachments = getBacklogAttachments(redmineWikiPage.attachments))
    }

    private def getBacklogAttachments(attachments: Seq[RedmineAttachment]): Seq[BacklogAttachment] =
      attachments.map(getBacklogAttachment)

    private def getBacklogAttachment(attachment: RedmineAttachment): BacklogAttachment =
      BacklogAttachment(attachment.id, attachment.fileName)
  }

  object IssueTypes {
    def apply(trackers: Seq[RedmineTracker]): BacklogIssueTypesWrapper = {
      val backlogIssueTypes: Seq[BacklogIssueType] = trackers.map(getBacklogIssueType)
      BacklogIssueTypesWrapper(backlogIssueTypes)
    }

    private def getBacklogIssueType(tracker: RedmineTracker): BacklogIssueType =
      BacklogIssueType(name = tracker.name, color = BacklogConfigBase.Backlog.ISSUE_TYPE_COLOR.getStrValue)
  }

  object Versions {
    def apply(versions: Seq[RedmineVersion]): BacklogVersionsWrapper = {
      val backlogVersions: Seq[BacklogVersion] = versions.map(getBacklogVersion)
      BacklogVersionsWrapper(backlogVersions)
    }

    private def getBacklogVersion(redmineVersion: RedmineVersion): BacklogVersion =
      BacklogVersion(
        name = redmineVersion.name,
        description = redmineVersion.description,
        startDate = None,
        releaseDueDate = redmineVersion.dueDate)
  }

  object IssueCategories {
    def apply(redmineIssueCategories: Seq[RedmineIssueCategory]): BacklogIssueCategoriesWrapper = {
      val categories: Seq[String] = redmineIssueCategories.map(_.name)
      BacklogIssueCategoriesWrapper(categories)
    }
  }

  object Projects {
    def apply(redmineProjects: Seq[RedmineProject], projectKeys: Seq[ParamProjectKey]): BacklogProjectsWrapper = {
      val backlogProjects: Seq[BacklogProject] = redmineProjects.map(project => getBacklogProject(project, projectKeys))
      BacklogProjectsWrapper(backlogProjects)
    }

    private def getBacklogProject(redmineProject: RedmineProject, projectKeys: Seq[ParamProjectKey]): BacklogProject =
      BacklogProject(
        id = redmineProject.id,
        name = redmineProject.name,
        key = convertProjectKey(redmineProject.identifier, projectKeys))

    private def convertProjectKey(redmineIdentifier: String, paramProjectKeys: Seq[ParamProjectKey]): String = {
      val paramProjectKey: Option[ParamProjectKey] = paramProjectKeys.find(projectKey => projectKey.redmine == redmineIdentifier)
      paramProjectKey match {
        case Some(projectKey) => projectKey.getBacklogKey()
        case None => redmineIdentifier.toUpperCase.replaceAll("-", "_")
      }
    }

  }

  object Groups {
    def apply(redmineGroups: Seq[RedmineGroup], redmineUsers: Seq[RedmineUser]): BacklogGroupsWrapper = {
      val backlogGroups: Seq[BacklogGroup] = redmineGroups.map(redmineGroup => getBacklogGroup(redmineGroup, redmineUsers))
      BacklogGroupsWrapper(backlogGroups)
    }

    private def getBacklogGroup(redmineGroup: RedmineGroup, redmineUsers: Seq[RedmineUser]): BacklogGroup = {
      val groupUserIds: Seq[String] = getUserIdsByGroupId(redmineGroup.name, redmineUsers)
      BacklogGroup(redmineGroup.name, groupUserIds.map(userMapping.convert))
    }

    private def getUserIdsByGroupId(groupName: String, redmineUsers: Seq[RedmineUser]): Seq[String] =
      redmineUsers.filter(redmineUser => redmineUser.groups.contains(groupName)).map(_.login)
  }

}
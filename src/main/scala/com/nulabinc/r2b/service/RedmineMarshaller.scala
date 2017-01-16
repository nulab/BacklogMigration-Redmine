package com.nulabinc.r2b.service

import java.util.Locale

import com.nulabinc.r2b.domain._
import com.osinka.i18n.Lang
import com.taskadapter.redmineapi.bean._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
object RedmineMarshaller {
  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  import RedmineJsonProtocol._

  val dateFormat = "yyyy-MM-dd"
  val parserISO = ISODateTimeFormat.dateTimeNoMillis()

  object Issue {
    def apply(issue: Issue, project: Project, users: Seq[User]): String =
      RedmineIssue(
        id = issue.getId.toInt,
        parentIssueId = Option(issue.getParentId).map(_.toInt),
        project = getRedmineProject(project),
        subject = issue.getSubject,
        description = issue.getDescription,
        startDate = Option(issue.getStartDate).map(date => new DateTime(date).toString(dateFormat)),
        dueDate = Option(issue.getDueDate).map(date => new DateTime(date).toString(dateFormat)),
        estimatedHours = Option(issue.getEstimatedHours).map(_.toDouble),
        spentHours = Option(issue.getSpentHours).map(_.toDouble),
        doneRatio = issue.getDoneRatio,
        status = issue.getStatusName,
        priority = issue.getPriorityText,
        tracker = issue.getTracker.getName,
        category = Option(issue.getCategory).map(_.getName),
        version = Option(issue.getTargetVersion).map(_.getName),
        assigneeId = getUserLogin(Option(issue.getAssignee), users),
        attachments = getRedmineAttachments(issue),
        journals = getRedmineJournals(issue, users),
        customFields = getRedmineCustomFields(issue),
        author = getUserLogin(Option(issue.getAuthor), users),
        createdOn = Option(issue.getCreatedOn).map(date => parserISO.print(new DateTime(date))),
        updatedOn = Option(issue.getUpdatedOn).map(date => parserISO.print(new DateTime(date)))).toJson.prettyPrint

    private[this] def getRedmineAttachments(issue: Issue): Seq[RedmineAttachment] = {
      val attachments: Array[Attachment] = issue.getAttachments.toArray(new Array[Attachment](issue.getAttachments.size()))
      attachments.map(getRedmineAttachment)
    }

    private[this] def getRedmineJournals(issue: Issue, users: Seq[User]): Seq[RedmineJournal] = {
      val journals: Array[Journal] = issue.getJournals.toArray(new Array[Journal](issue.getJournals.size()))
      journals.map(journal => getRedmineJournal(journal, users))
    }

    private[this] def getRedmineCustomFields(issue: Issue): Seq[RedmineCustomField] = {
      val customFields: Array[CustomField] = issue.getCustomFields.toArray(new Array[CustomField](issue.getCustomFields.size()))
      customFields.map(getRedmineCustomField)
    }
  }

  object CustomFieldDefinition {
    def apply(customFields: Seq[RedmineCustomFieldDefinition]): String = {
      RedmineCustomFieldDefinitionsWrapper(customFields).toJson.prettyPrint
    }
  }

  object Wiki {
    def apply(wikiPageDetail: WikiPageDetail, users: Seq[User]): String = {
      RedmineWikiPage(
        title = wikiPageDetail.getTitle,
        text = Option(wikiPageDetail.getText),
        user = getUserLogin(Option(wikiPageDetail.getUser), users),
        comments = Option(wikiPageDetail.getComments),
        parentTitle = Option(wikiPageDetail.getParent).map(_.getTitle),
        createdOn = Option(wikiPageDetail.getCreatedOn).map(date => parserISO.print(new DateTime(date))),
        updatedOn = Option(wikiPageDetail.getUpdatedOn).map(date => parserISO.print(new DateTime(date))),
        attachments = getRedmineAttachments(wikiPageDetail)).toJson.prettyPrint
    }

    private[this] def getRedmineAttachments(wikiPageDetail: WikiPageDetail): Seq[RedmineAttachment] = {
      val attachments: Array[Attachment] = wikiPageDetail.getAttachments.toArray(new Array[Attachment](wikiPageDetail.getAttachments.size()))
      attachments.map(getRedmineAttachment)
    }
  }

  object Users {
    def apply(users: Seq[User]): String =
      RedmineUsersWrapper(users.map(getRedmineUser)).toJson.prettyPrint
  }

  object Project {
    def apply(project: Project): String =
      RedmineProjectsWrapper(getRedmineProject(project)).toJson.prettyPrint
  }

  object News {
    def apply(news: Seq[News], users: Seq[User]): String =
      RedmineNewsWrapper(news.map(news => getRedmineNews(news, users))).toJson.prettyPrint

    private[this] def getRedmineNews(news: News, users: Seq[User]): RedmineNews =
      RedmineNews(
        id = news.getId,
        title = news.getTitle,
        description = news.getDescription,
        link = Option(news.getLink),
        user = getUserLogin(Option(news.getUser), users),
        createdOn = Option(news.getCreatedOn).map(date => parserISO.print(new DateTime(date))))
  }

  object Membership {
    def apply(memberships: Seq[Membership]): String = {
      val redmineUsers: Seq[RedmineUser] = memberships.filter(_.getUser != null).map(membership => getRedmineUser(membership.getUser))
      RedmineMembershipsWrapper(redmineUsers).toJson.prettyPrint
    }
  }

  object IssueCategory {
    def apply(categories: Seq[IssueCategory]): String = {
      val redmineIssueCategories: Seq[RedmineIssueCategory] = categories.map(getRedmineIssueCategory)
      RedmineIssueCategoriesWrapper(redmineIssueCategories).toJson.prettyPrint
    }

    def getRedmineIssueCategory(category: IssueCategory): RedmineIssueCategory =
      RedmineIssueCategory(category.getId, category.getName)
  }

  object Versions {
    def apply(versions: Seq[Version]): String = {
      val redmineVersions: Seq[RedmineVersion] = versions.map(getRedmineVersion)
      RedmineVersionsWrapper(redmineVersions).toJson.prettyPrint
    }

    def getRedmineVersion(version: Version): RedmineVersion =
      RedmineVersion(
        id = version.getId,
        name = version.getName,
        description = version.getDescription,
        dueDate = Option(version.getDueDate).map(date => new DateTime(date).toString(dateFormat)),
        createdOn = dateFormat.format(version.getCreatedOn))
  }

  object Tracker {
    def apply(trackers: Seq[Tracker]): String = {
      val redmineTrackers: Seq[RedmineTracker] = trackers.map(getRedmineTracker)
      RedmineTrackersWrapper(redmineTrackers).toJson.prettyPrint
    }
  }

  object IssueStatus {
    def apply(issueStatuses: Seq[IssueStatus]): String = {
      RedmineIssueStatusesWrapper(issueStatuses.map(getRedmineIssueStatus)).toJson.prettyPrint
    }

    def getRedmineIssueStatus(issueStatus: IssueStatus): RedmineIssueStatus =
      RedmineIssueStatus(issueStatus.getId, issueStatus.getName)
  }

  object IssuePriority {
    def apply(issuePriorities: Seq[IssuePriority]): String = {
      RedminePrioritiesWrapper(issuePriorities.map(getRedminePriority)).toJson.prettyPrint
    }

    def getRedminePriority(issuePriority: IssuePriority): RedminePriority =
      RedminePriority(issuePriority.getId, issuePriority.getName)
  }

  object Group {
    def apply(groups: Seq[Group]): String = {
      val redmineGroups: Seq[RedmineGroup] = groups.map(getRedmineGroup)
      RedmineGroupsWrapper(redmineGroups).toJson.prettyPrint
    }

    private[this] def getRedmineGroup(group: Group): RedmineGroup = RedmineGroup(group.getId, group.getName)
  }

  private[this] def getRedmineProject(project: Project): RedmineProject =
    RedmineProject(
      id = project.getId,
      name = project.getName,
      identifier = project.getIdentifier)

  private[this] def getRedmineAttachment(attachment: Attachment): RedmineAttachment =
    RedmineAttachment(
      id = attachment.getId,
      fileName = attachment.getFileName)

  private[this] def getRedmineCustomField(customField: CustomField): RedmineCustomField =
    RedmineCustomField(
      id = customField.getId,
      name = customField.getName,
      value = Option(customField.getValue).filter(!_.isEmpty),
      multiple = customField.isMultiple,
      values = if (customField.getValues == null) Seq.empty[String] else customField.getValues.asScala)

  private[this] def getRedmineJournal(journal: Journal, users: Seq[User]): RedmineJournal =
    RedmineJournal(
      id = journal.getId,
      notes = Option(journal.getNotes),
      details = journal.getDetails.asScala.map(getRedmineJournalDetail),
      user = getUserLogin(Option(journal.getUser), users),
      createdOn = Option(journal.getCreatedOn).map(date => parserISO.print(new DateTime(date))))

  private[this] def getRedmineJournalDetail(journalDetail: JournalDetail): RedmineJournalDetail =
    RedmineJournalDetail(
      property = journalDetail.getProperty,
      name = journalDetail.getName,
      oldValue = if (journalDetail.getOldValue == null || journalDetail.getOldValue.isEmpty) None else Some(journalDetail.getOldValue),
      newValue = if (journalDetail.getNewValue == null || journalDetail.getNewValue.isEmpty) None else Some(journalDetail.getNewValue))

  private[this] def getRedmineUser(user: User): RedmineUser =
    RedmineUser(
      id = user.getId,
      firstname = user.getFirstName,
      lastname = user.getLastName,
      fullname = user.getFullName,
      login = Option(user.getLogin).getOrElse(""),
      mail = Option(user.getMail),
      groups = user.getGroups.asScala.map(_.getName).toSeq)

  private[this] def getUserLogin(optUser: Option[User], users: Seq[User]): Option[String] = optUser match {
    case Some(targetUser) =>
      users.find(user => user.getId == targetUser.getId).map(_.getLogin)
    case None => None
  }

  private[this] def getRedmineTracker(tracker: Tracker): RedmineTracker =
    RedmineTracker(tracker.getId, tracker.getName)

}
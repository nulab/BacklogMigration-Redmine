package com.nulabinc.r2b.domain

import spray.json.DefaultJsonProtocol

/**
  * @author uchida
  */
case class RedmineCustomField(
                               id: Int,
                               name: String,
                               value: Option[String],
                               multiple: Boolean,
                               values: Seq[String])

case class RedmineIssue(
                         id: Int,
                         parentIssueId: Option[Int],
                         project: RedmineProject,
                         subject: String,
                         description: String,
                         startDate: Option[String],
                         dueDate: Option[String],
                         estimatedHours: Option[Double],
                         spentHours: Option[Double],
                         doneRatio: Int,
                         status: String,
                         priority: String,
                         tracker: String,
                         category: Option[String],
                         version: Option[String],
                         assigneeId: Option[String],
                         attachments: Seq[RedmineAttachment],
                         journals: Seq[RedmineJournal],
                         customFields: Seq[RedmineCustomField],
                         author: Option[String],
                         createdOn: Option[String],
                         updatedOn: Option[String])

case class RedmineIssuesWrapper(total_count: Int, offset: Int, limit: Int)

case class RedmineJournal(
                           id: Int,
                           notes: Option[String],
                           details: Seq[RedmineJournalDetail],
                           user: Option[String],
                           createdOn: Option[String])

case class RedmineJournalDetail(property: String, name: String, oldValue: Option[String], newValue: Option[String])

case class RedmineAttachment(
                              id: Int,
                              fileName: String)

case class RedmineWikiPage(
                            title: String,
                            text: Option[String],
                            user: Option[String],
                            comments: Option[String],
                            parentTitle: Option[String],
                            createdOn: Option[String],
                            updatedOn: Option[String],
                            attachments: Seq[RedmineAttachment])

case class RedmineProject(id: Int, name: String, identifier: String)

case class RedmineProjectsWrapper(projects: Seq[RedmineProject])

case class RedmineUser(
                        id: Int,
                        firstname: String,
                        lastname: String,
                        fullname: String,
                        login: String,
                        mail: Option[String],
                        groups: Seq[String])

case class RedmineNews(
                        id: Int,
                        title: String,
                        description: String,
                        link: Option[String],
                        user: Option[String],
                        createdOn: Option[String])

case class RedmineNewsWrapper(news: Seq[RedmineNews])

case class RedmineUsersWrapper(users: Seq[RedmineUser])

case class RedmineGroup(id: Int, name: String)

case class RedmineGroupsWrapper(groups: Seq[RedmineGroup])

case class RedmineMembershipsWrapper(users: Seq[RedmineUser])

case class RedmineTracker(id: Int, name: String)

case class RedmineTrackersWrapper(trackers: Seq[RedmineTracker])

case class RedminePriority(id: Int, name: String)

case class RedminePrioritiesWrapper(priorities: Seq[RedminePriority])

case class RedmineIssueCategory(id: Int, name: String)

case class RedmineIssueCategoriesWrapper(categories: Seq[RedmineIssueCategory])

case class RedmineIssueStatus(id: Int, name: String)

case class RedmineIssueStatusesWrapper(issueStatuses: Seq[RedmineIssueStatus])

case class RedmineVersion(id: Int, name: String, description: String, dueDate: Option[String], createdOn: String)

case class RedmineVersionsWrapper(versions: Seq[RedmineVersion])

case class RedmineCustomFieldDefinition(
                                         id: Int,
                                         name: String,
                                         customizedType: String,
                                         fieldFormat: String,
                                         regexp: Option[String],
                                         minLength: Option[Int],
                                         maxLength: Option[Int],
                                         isRequired: Boolean,
                                         isMultiple: Boolean,
                                         defaultValue: Option[String],
                                         trackers: Seq[RedmineTracker],
                                         possibleValues: Seq[String])

case class RedmineCustomFieldDefinitionsWrapper(customFields: Seq[RedmineCustomFieldDefinition])

case class OldTrackers(tracker: Option[RedmineTracker])

case class OldCustomFieldDefinition(
                                     id: Int,
                                     name: String,
                                     customized_type: String,
                                     field_format: String,
                                     regexp: Option[String],
                                     min_length: Option[Int],
                                     max_length: Option[Int],
                                     default_value: Option[String],
                                     trackers: OldTrackers,
                                     possible_values: Option[Seq[OldPossibleValues]])

case class OldPossibleValues(value: String)

case class OldCustomFieldDefinitionsWrapper(custom_fields: Seq[OldCustomFieldDefinition])

object RedmineJsonProtocol extends DefaultJsonProtocol {
  implicit val RedmineProjectFormat = jsonFormat3(RedmineProject)
  implicit val RedmineIssueStatusFormat = jsonFormat2(RedmineIssueStatus)
  implicit val RedmineIssueStatusesWrapperFormat = jsonFormat1(RedmineIssueStatusesWrapper)
  implicit val RedmineAttachmentFormat = jsonFormat2(RedmineAttachment)
  implicit val RedmineWikiPageFormat = jsonFormat8(RedmineWikiPage)
  implicit val RedmineCustomFieldFormat = jsonFormat5(RedmineCustomField)
  implicit val RedmineJournalDetailFormat = jsonFormat4(RedmineJournalDetail)
  implicit val RedmineJournalFormat = jsonFormat5(RedmineJournal)
  implicit val RedmineIssueFormat = jsonFormat22(RedmineIssue)
  implicit val RedmineProjectsWrapperFormat = jsonFormat1(RedmineProjectsWrapper)
  implicit val RedmineUserFormat = jsonFormat7(RedmineUser)
  implicit val RedmineUsersWrapperFormat = jsonFormat1(RedmineUsersWrapper)
  implicit val RedmineGroupFormat = jsonFormat2(RedmineGroup)
  implicit val RedmineGroupWrapperFormat = jsonFormat1(RedmineGroupsWrapper)
  implicit val RedmineMembershipsWrapperFormat = jsonFormat1(RedmineMembershipsWrapper)
  implicit val RedmineTrackerFormat = jsonFormat2(RedmineTracker)
  implicit val RedmineTrackersWrapperFormat = jsonFormat1(RedmineTrackersWrapper)
  implicit val RedminePriorityFormat = jsonFormat2(RedminePriority)
  implicit val RedminePrioritiesWrapperFormat = jsonFormat1(RedminePrioritiesWrapper)
  implicit val RedmineIssueCategoryFormat = jsonFormat2(RedmineIssueCategory)
  implicit val RedmineIssueCategoriesWrapperFormat = jsonFormat1(RedmineIssueCategoriesWrapper)
  implicit val RedmineCustomFieldDefinitionFormat = jsonFormat12(RedmineCustomFieldDefinition)
  implicit val RedmineCustomFieldDefinitionsWrapperFormat = jsonFormat1(RedmineCustomFieldDefinitionsWrapper)
  implicit val RedmineNewsFormat = jsonFormat6(RedmineNews)
  implicit val RedmineNewsWrapperFormat = jsonFormat1(RedmineNewsWrapper)
  implicit val RedmineVersionFormat = jsonFormat5(RedmineVersion)
  implicit val RedmineVersionsWrapperFormat = jsonFormat1(RedmineVersionsWrapper)
  implicit val RedmineIssuesWrapperFormat = jsonFormat3(RedmineIssuesWrapper)
  implicit val OldPossibleValuesFormat = jsonFormat1(OldPossibleValues)
  implicit val OldTrackersFormat = jsonFormat1(OldTrackers)
  implicit val OldCustomFieldDefinitionFormat = jsonFormat10(OldCustomFieldDefinition)
  implicit val OldCustomFieldDefinitionsWrapperFormat = jsonFormat1(OldCustomFieldDefinitionsWrapper)
}
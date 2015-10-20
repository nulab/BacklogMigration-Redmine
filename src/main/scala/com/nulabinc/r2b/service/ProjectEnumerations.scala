package com.nulabinc.r2b.service

import com.nulabinc.r2b.domain._

/**
 * @author uchida
 */
class ProjectEnumerations(identifier: String) {

  private val statusMapping: ConvertStatusMapping = new ConvertStatusMapping()
  private val priorityMapping: ConvertPriorityMapping = new ConvertPriorityMapping()

  private val optionCustomFieldDefinitions: Option[Seq[RedmineCustomFieldDefinition]] = RedmineUnmarshaller.customFieldDefinitions()
  private val optionUsers: Option[Seq[RedmineUser]] = RedmineUnmarshaller.users()
  private val optionStatuses: Option[Seq[RedmineIssueStatus]] = RedmineUnmarshaller.statuses()
  private val optionPriorities: Option[Seq[RedminePriority]] = RedmineUnmarshaller.priorities()
  private val optionTrackers: Option[Seq[RedmineTracker]] = RedmineUnmarshaller.trackers()
  private val optionCategories: Option[Seq[RedmineIssueCategory]] = RedmineUnmarshaller.categories(identifier)
  private val optionVersions: Option[Seq[RedmineVersion]] = RedmineUnmarshaller.versions(identifier)
  private val optionMembershipsUsers: Option[Seq[RedmineUser]] = RedmineUnmarshaller.membershipsUsers(identifier)

  object CustomFieldDefinitions {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        customFieldDefinitions <- optionCustomFieldDefinitions} yield {
        val id: Int = strId.toInt
        convert(id, customFieldDefinitions)
      }
      result.flatten
    }

    private def convert(id: Int, customFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Option[String] =
      customFieldDefinitions.find(customFieldDefinition => customFieldDefinition.id == id).map(_.name)
  }

  object User {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        users <- optionUsers} yield {
        val id: Int = strId.toInt
        convert(id, users)
      }
      result.flatten
    }

    private def convert(id: Int, users: Seq[RedmineUser]): Option[String] =
      users.find(user => user.id == id).map(_.login)
  }

  object UserId {
    def convertValue(userId: Option[String]): Option[String] = {
      val result = for {login <- userId
                        users <- optionUsers} yield {
        convert(login, users)
      }
      result.flatten
    }

    private def convert(login: String, users: Seq[RedmineUser]): Option[String] =
      users.find(user => user.login == login).map(_.fullname)
  }

  object IssueStatus {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        statuses <- optionStatuses} yield {
        val id: Int = strId.toInt
        convert(id, statuses).map(statusMapping.convert)
      }
      result.flatten
    }

    private def convert(id: Int, statuses: Seq[RedmineIssueStatus]): Option[String] =
      statuses.find(status => status.id == id).map(_.name)
  }

  object Priority {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        priorities <- optionPriorities} yield {
        val id: Int = strId.toInt
        convert(id, priorities).map(priorityMapping.convert)
      }
      result.flatten
    }

    private def convert(id: Int, priorities: Seq[RedminePriority]): Option[String] =
      priorities.find(priority => priority.id == id).map(_.name)
  }

  object Tracker {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        trackers <- optionTrackers} yield {
        val id: Int = strId.toInt
        convert(id, trackers)
      }
      result.flatten
    }

    private def convert(id: Int, trackers: Seq[RedmineTracker]): Option[String] =
      trackers.find(tracker => tracker.id == id).map(_.name)
  }

  object Category {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        categories <- optionCategories} yield {
        val id: Int = strId.toInt
        convert(id, categories)
      }
      result.flatten
    }

    private def convert(id: Int, categories: Seq[RedmineIssueCategory]): Option[String] =
      categories.find(category => category.id == id).map(_.name)
  }

  object Version {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        versions <- optionVersions} yield {
        val id: Int = strId.toInt
        convert(id, versions)
      }
      result.flatten
    }

    private def convert(id: Int, versions: Seq[RedmineVersion]): Option[String] =
      versions.find(version => version.id == id).map(_.name)

    def values() = optionVersions.getOrElse(Seq.empty[RedmineVersion]).map(_.name)

  }

  object Membership {
    def convertValue(id: Option[String]): Option[String] = {
      val result = for {strId <- id
                        membershipsUsers <- optionMembershipsUsers} yield {
        val id: Int = strId.toInt
        convert(id, membershipsUsers)
      }
      result.flatten
    }

    private def convert(id: Int, membershipsUsers: Seq[RedmineUser]): Option[String] =
      membershipsUsers.find(user => user.id == id).map(user => user.fullname)

    def values() = optionMembershipsUsers.getOrElse(Seq.empty[RedmineUser]).map(_.fullname)
  }

}

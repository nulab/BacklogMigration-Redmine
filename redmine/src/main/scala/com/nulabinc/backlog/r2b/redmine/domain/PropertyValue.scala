package com.nulabinc.backlog.r2b.redmine.domain

import com.nulabinc.backlog.migration.common.utils.StringUtil
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
case class PropertyValue(users: Seq[User],
                         versions: Seq[Version],
                         categories: Seq[IssueCategory],
                         priorities: Seq[IssuePriority],
                         trackers: Seq[Tracker],
                         memberships: Seq[Membership],
                         statuses: Seq[IssueStatus],
                         customFieldDefinitions: Seq[RedmineCustomFieldDefinition]) {

  def versionOfId(optValue: Option[String]): Option[Version] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => versions.find(version => version.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def priorityOfId(optValue: Option[String]): Option[IssuePriority] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => priorities.find(priority => priority.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def userOfId(optValue: Option[String]): Option[User] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => users.find(user => user.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def optUserOfId(id: Int): Option[User] =
    users.find(user => user.getId.intValue() == id) match {
      case Some(user) => Some(user)
      case _          => None
    }

  def optUserOfId(optValue: Option[String]): Option[User] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => users.find(user => user.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def categoryOfId(optValue: Option[String]): Option[IssueCategory] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => categories.find(category => category.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def trackerOfId(optValue: Option[String]): Option[Tracker] =
    optValue match {
      case Some(value) =>
        StringUtil.safeStringToInt(value) match {
          case Some(intValue) => trackers.find(tracker => tracker.getId.intValue() == intValue)
          case _              => None
        }
      case _ => None
    }

  def customFieldDefinitionOfId(strId: String): Option[RedmineCustomFieldDefinition] = {
    StringUtil.safeStringToInt(strId) match {
      case Some(id) =>
        customFieldDefinitions.find(customFieldDefinition => customFieldDefinition.id == id)
      case _ => None
    }
  }

  def customFieldDefinitionOfName(name: String): Option[RedmineCustomFieldDefinition] = {
    customFieldDefinitions.find(customFieldDefinition => customFieldDefinition.name == name)
  }

}

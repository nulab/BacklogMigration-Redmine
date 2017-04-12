package com.nulabinc.r2b.redmine.domain

import com.nulabinc.backlog.migration.utils.StringUtil
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
                         statuses: Seq[IssueStatus]) {

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

  def userOfId(id: Int): User =
    users.find(user => user.getId.intValue() == id) match {
      case Some(user) => user
      case _          => throw new RuntimeException(s"user not found.[${id}]")
    }

  def optUserOfId(id: Int): Option[User] = {
    users.find(user => user.getId.intValue() == id)
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

}

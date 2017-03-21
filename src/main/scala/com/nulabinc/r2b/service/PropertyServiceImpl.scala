package com.nulabinc.r2b.service

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.Issue.StatusType
import com.nulabinc.backlog4j.{BacklogClient, Status}
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean._

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class PropertyServiceImpl(
                           projectId: Int,
                           redmine: RedmineManager,
                           backlog: BacklogClient,
                           statusMapping: ConvertStatusMapping,
                           priorityMapping: ConvertPriorityMapping) extends PropertyService with Logging {

  private[this] var users = redmine.getUserManager.getUsers.asScala.map(user => redmine.getUserManager.getUserById(user.getId))

  private[this] var statuses = redmine.getIssueManager.getStatuses.asScala

  private[this] var priorities = redmine.getIssueManager.getIssuePriorities.asScala

  private[this] var trackers = redmine.getIssueManager.getTrackers.asScala

  private[this] var projects: Seq[Project] = redmine.getProjectManager.getProjects.asScala

  private[this] var optCategories: Option[Seq[IssueCategory]] = None

  private[this] var optVersions: Option[Seq[Version]] = None

  private[this] var optMemberships: Option[Seq[Membership]] = None

  private[this] val backlogStatuses: Seq[Status] = backlog.getStatuses.asScala

  override def reload() = {
    users = redmine.getUserManager.getUsers.asScala.map(user => redmine.getUserManager.getUserById(user.getId))
    statuses = redmine.getIssueManager.getStatuses.asScala
    priorities = redmine.getIssueManager.getIssuePriorities.asScala
    trackers = redmine.getIssueManager.getTrackers.asScala
    optCategories = optAllCategories()
    optVersions = optAllVersions()
    optMemberships = optAllMemberships()
  }

  override def optUser(id: Int): Option[String] =
    users.find(_.getId.intValue() == id).map(_.getLogin)

  override def optUser(optId: Option[String]): Option[String] =
    (for {id <- optId} yield
      users.find(_.getId.intValue() == id.toInt).map(_.getLogin)).flatten

  override def optUserName(optId: Option[String]): Option[String] =
    (for {id <- optId} yield {
      users.find(_.getId.intValue() == id.toInt).map(_.getFullName)
    }).flatten

  override def allVersionNames(): Seq[String] = {
    if (optVersions.isEmpty) optVersions = optAllVersions()
    optVersions match {
      case Some(versions) => versions.map(_.getName)
      case None => Seq.empty[String]
    }
  }

  override def optVersionName(optId: Option[String]): Option[String] = {
    if (optVersions.isEmpty) optVersions = optAllVersions()
    (for {
      versions <- optVersions
      id <- optId
    } yield versions.find(_.getId.intValue() == id.toInt).map(_.getName)).flatten
  }

  override def allMembershipNames(): Seq[String] = {
    if (optMemberships.isEmpty) optMemberships = optAllMemberships()
    optMemberships match {
      case Some(memberships) => memberships.filter(_.getUser != null).map(_.getUser.getFullName)
      case None => Seq.empty[String]
    }
  }

  override def optMembershipName(optId: Option[String]): Option[String] = {
    if (optMemberships.isEmpty) optMemberships = optAllMemberships()
    (for {
      memberships <- optMemberships
      id <- optId
    } yield memberships.filter(membership => Option(membership.getUser).nonEmpty).find(_.getUser.getId.intValue() == id.toInt).map(_.getUser.getFullName)).flatten
  }

  override def optStatusName(optId: Option[String]): Option[String] =
    (for {
      id <- optId
    } yield {
      statuses.find(_.getId.intValue() == id.toInt).map(_.getName)
    }).flatten.map(statusMapping.convert)

  override def optTrackerName(optId: Option[String]): Option[String] =
    (for {id <- optId} yield
      trackers.find(_.getId.intValue() == id.toInt).map(_.getName)).flatten

  override def optCategoryName(optId: Option[String]): Option[String] = {
    if (optCategories.isEmpty) optCategories = optAllCategories()
    (for {
      categories <- optCategories
      id <- optId
    } yield
      categories.find(_.getId.intValue() == id.toInt).map(_.getName)).flatten
  }

  override def optDefaultStatusName(): Option[String] = {
    backlogStatuses.find(_.getStatusType == StatusType.Open).map(_.getName)
  }

  override def optPriorityName(optId: Option[String]): Option[String] =
    (for {id <- optId} yield {
      priorities.find(_.getId.intValue() == id.toInt).map(_.getName)
    }).flatten.map(priorityMapping.convert)

  override def optProjectName(id: Int): Option[String] = projects.find(_.getId.intValue() == id).map(_.getName)

  private[this] def optAllCategories(): Option[Seq[IssueCategory]] =
    try {
      Some(redmine.getIssueManager.getCategories(projectId).asScala)
    } catch {
      case e: Throwable =>
        log.error(e)
        None
    }

  private[this] def optAllVersions(): Option[Seq[Version]] =
    try {
      Some(redmine.getProjectManager.getVersions(projectId).asScala)
    } catch {
      case e: Throwable =>
        log.error(e)
        None
    }

  private[this] def optAllMemberships(): Option[Seq[Membership]] =
    try {
      Some(redmine.getMembershipManager.getMemberships(projectId).asScala)
    } catch {
      case e: Throwable =>
        log.error(e)
        None
    }

}

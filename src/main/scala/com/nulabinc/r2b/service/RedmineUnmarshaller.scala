package com.nulabinc.r2b.service

import com.nulabinc.backlog.migration.utils.IOUtil
import com.nulabinc.r2b.conf.RedmineDirectory
import com.nulabinc.r2b.domain._
import spray.json.JsonParser

/**
  * @author uchida
  */
object RedmineUnmarshaller {

  import RedmineJsonProtocol._

  def issue(path: String): Option[RedmineIssue] = IOUtil.input(path).map(JsonParser(_).convertTo[RedmineIssue])

  def users(redmineDirectory: RedmineDirectory): Option[Seq[RedmineUser]] =
    IOUtil.input(redmineDirectory.USERS).map(json => {
      val redmineUsersWrapper: RedmineUsersWrapper = JsonParser(json).convertTo[RedmineUsersWrapper]
      redmineUsersWrapper.users
    })

  def groups(redmineDirectory: RedmineDirectory): Option[Seq[RedmineGroup]] =
    IOUtil.input(redmineDirectory.GROUP_USERS).map(json => {
      val redmineGroupsWrapper: RedmineGroupsWrapper = JsonParser(json).convertTo[RedmineGroupsWrapper]
      redmineGroupsWrapper.groups
    })

  def customFieldDefinitions(redmineDirectory: RedmineDirectory): Option[Seq[RedmineCustomFieldDefinition]] =
    IOUtil.input(redmineDirectory.CUSTOM_FIELDS).map(json => {
      val redmineCustomFieldsWrapper: RedmineCustomFieldDefinitionsWrapper = JsonParser(json).convertTo[RedmineCustomFieldDefinitionsWrapper]
      redmineCustomFieldsWrapper.customFields
    })

  def news(redmineDirectory: RedmineDirectory): Option[Seq[RedmineNews]] =
    IOUtil.input(redmineDirectory.getNewsPath()).map(json => {
      val redmineNewsWrapper: RedmineNewsWrapper = JsonParser(json).convertTo[RedmineNewsWrapper]
      redmineNewsWrapper.news
    })

  def project(redmineDirectory: RedmineDirectory): Option[RedmineProject] =
    IOUtil.input(redmineDirectory.PROJECTS).map(json => {
      val redmineProjectsWrapper: RedmineProjectsWrapper = JsonParser(json).convertTo[RedmineProjectsWrapper]
      redmineProjectsWrapper.project
    })

  def membershipsUsers(redmineDirectory: RedmineDirectory): Option[Seq[RedmineUser]] =
    IOUtil.input(redmineDirectory.getMembershipsPath()).map(json => {
      val redmineMembershipsWrapper: RedmineMembershipsWrapper = JsonParser(json).convertTo[RedmineMembershipsWrapper]
      redmineMembershipsWrapper.users
    })

  def categories(redmineDirectory: RedmineDirectory): Option[Seq[RedmineIssueCategory]] =
    IOUtil.input(redmineDirectory.getIssueCategoriesPath()).map(json => {
      val redmineIssueCategoriesWrapper: RedmineIssueCategoriesWrapper = JsonParser(json).convertTo[RedmineIssueCategoriesWrapper]
      redmineIssueCategoriesWrapper.categories
    })

  def trackers(redmineDirectory: RedmineDirectory): Option[Seq[RedmineTracker]] =
    IOUtil.input(redmineDirectory.TRACKERS).map(json => {
      val redmineTrackersWrapper: RedmineTrackersWrapper = JsonParser(json).convertTo[RedmineTrackersWrapper]
      redmineTrackersWrapper.trackers
    })

  def versions(redmineDirectory: RedmineDirectory): Option[Seq[RedmineVersion]] =
    IOUtil.input(redmineDirectory.getVersionsPath()).map(json => {
      val redmineVersionsWrapper: RedmineVersionsWrapper = JsonParser(json).convertTo[RedmineVersionsWrapper]
      redmineVersionsWrapper.versions
    })

  def wiki(path: String): Option[RedmineWikiPage] =
    IOUtil.input(path).map(JsonParser(_).convertTo[RedmineWikiPage])

  def statuses(redmineDirectory: RedmineDirectory): Option[Seq[RedmineIssueStatus]] =
    IOUtil.input(redmineDirectory.ISSUE_STATUSES).map(json => {
      val redmineIssueStatusesWrapper: RedmineIssueStatusesWrapper = JsonParser(json).convertTo[RedmineIssueStatusesWrapper]
      redmineIssueStatusesWrapper.issueStatuses
    })

  def priorities(redmineDirectory: RedmineDirectory): Option[Seq[RedminePriority]] =
    IOUtil.input(redmineDirectory.PRIORITY).map(json => {
      val redminePrioritiesWrapper: RedminePrioritiesWrapper = JsonParser(json).convertTo[RedminePrioritiesWrapper]
      redminePrioritiesWrapper.priorities
    })

}

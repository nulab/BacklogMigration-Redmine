package com.nulabinc.r2b.service

import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.utils.IOUtil
import spray.json.JsonParser

/**
 * @author uchida
 */
object RedmineUnmarshaller {

  import RedmineJsonProtocol._

  def issue(path: String): Option[RedmineIssue] = IOUtil.input(path).map(JsonParser(_).convertTo[RedmineIssue])

  def users(): Option[Seq[RedmineUser]] =
    IOUtil.input(ConfigBase.Redmine.USERS).map(json => {
      val redmineUsersWrapper: RedmineUsersWrapper = JsonParser(json).convertTo[RedmineUsersWrapper]
      redmineUsersWrapper.users
    })


  def groups(): Option[Seq[RedmineGroup]] =
    IOUtil.input(ConfigBase.Redmine.GROUP_USERS).map(json => {
      val redmineGroupsWrapper: RedmineGroupsWrapper = JsonParser(json).convertTo[RedmineGroupsWrapper]
      redmineGroupsWrapper.groups
    })

  def customFieldDefinitions(): Option[Seq[RedmineCustomFieldDefinition]] =
    IOUtil.input(ConfigBase.Redmine.CUSTOM_FIELDS).map(json => {
      val redmineCustomFieldsWrapper: RedmineCustomFieldDefinitionsWrapper = JsonParser(json).convertTo[RedmineCustomFieldDefinitionsWrapper]
      redmineCustomFieldsWrapper.customFields
    })

  def news(identifier: String): Option[Seq[RedmineNews]] =
    IOUtil.input(ConfigBase.Redmine.getNewsPath(identifier)).map(json => {
      val redmineNewsWrapper: RedmineNewsWrapper = JsonParser(json).convertTo[RedmineNewsWrapper]
      redmineNewsWrapper.news
    })

  def projects(): Option[Seq[RedmineProject]] =
    IOUtil.input(ConfigBase.Redmine.PROJECTS).map(json => {
      val redmineProjectsWrapper: RedmineProjectsWrapper = JsonParser(json).convertTo[RedmineProjectsWrapper]
      redmineProjectsWrapper.projects
    })

  def membershipsUsers(identifier: String): Option[Seq[RedmineUser]] =
    IOUtil.input(ConfigBase.Redmine.getMembershipsPath(identifier)).map(json => {
      val redmineMembershipsWrapper: RedmineMembershipsWrapper = JsonParser(json).convertTo[RedmineMembershipsWrapper]
      redmineMembershipsWrapper.users
    })

  def categories(identifier: String): Option[Seq[RedmineIssueCategory]] =
    IOUtil.input(ConfigBase.Redmine.getIssueCategoriesPath(identifier)).map(json => {
      val redmineIssueCategoriesWrapper: RedmineIssueCategoriesWrapper = JsonParser(json).convertTo[RedmineIssueCategoriesWrapper]
      redmineIssueCategoriesWrapper.categories
    })

  def trackers(): Option[Seq[RedmineTracker]] =
    IOUtil.input(ConfigBase.Redmine.TRACKERS).map(json => {
      val redmineTrackersWrapper: RedmineTrackersWrapper = JsonParser(json).convertTo[RedmineTrackersWrapper]
      redmineTrackersWrapper.trackers
    })

  def versions(identifier: String): Option[Seq[RedmineVersion]] =
    IOUtil.input(ConfigBase.Redmine.getVersionsPath(identifier)).map(json => {
      val redmineVersionsWrapper: RedmineVersionsWrapper = JsonParser(json).convertTo[RedmineVersionsWrapper]
      redmineVersionsWrapper.versions
    })

  def wiki(path: String): Option[RedmineWikiPage] =
    IOUtil.input(path).map(JsonParser(_).convertTo[RedmineWikiPage])

  def statuses(): Option[Seq[RedmineIssueStatus]] =
    IOUtil.input(ConfigBase.Redmine.ISSUE_STATUSES).map(json => {
      val redmineIssueStatusesWrapper: RedmineIssueStatusesWrapper = JsonParser(json).convertTo[RedmineIssueStatusesWrapper]
      redmineIssueStatusesWrapper.issueStatuses
    })

  def priorities(): Option[Seq[RedminePriority]] =
    IOUtil.input(ConfigBase.Redmine.PRIORITY).map(json => {
      val redminePrioritiesWrapper: RedminePrioritiesWrapper = JsonParser(json).convertTo[RedminePrioritiesWrapper]
      redminePrioritiesWrapper.priorities
    })

}

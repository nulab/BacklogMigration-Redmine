package com.nulabinc.r2b.conf

import java.io.File

/**
 * @author uchida
 */
object ConfigBase {
  val LOG_APPLICATION_LABEL = "[R2B]"
  val NAME: String = "Backlog Migration for Redmine"
  val VERSION: String = "0.9.0b15"

  val root = new File(".").getAbsoluteFile.getParent
  val R2B_ROOT = root + "/backlog-migration-redmine"
  val USER_MAPPING_FILE = root + "/mapping/users.json"
  val STATUS_MAPPING_FILE = root + "/mapping/statuses.json"
  val PRIORITY_MAPPING_FILE = root + "/mapping/priorities.json"

  val WIKI_FILE_NAME: String = "wiki.json"
  val ISSUE_FILE_NAME: String = "issue.json"
  val DEFAULT_PASSWORD: String = "password"

  object FieldFormat {
    val VERSION: String = "version"
    val USER: String = "user"
    val STRING: String = "string"
    val LINK: String = "link"
    val INT: String = "int"
    val FLOAT: String = "float"
    val DATE: String = "date"
    val TEXT: String = "text"
    val LIST: String = "list"
    val BOOL: String = "bool"
  }

  object Property {
    val ATTACHMENT: String = "attachment"
    val CF: String = "cf"
    val ATTR: String = "attr"

    object Attr {
      val SUBJECT: String = "subject"
      val TRACKER: String = "tracker_id"
      val STATUS: String = "status_id"
      val PRIORITY: String = "priority_id"
      val ASSIGNED: String = "assigned_to_id"
      val VERSION: String = "fixed_version_id"
      val PARENT: String = "parent_id"
      val START_DATE: String = "start_date"
      val DUE_DATE: String = "due_date"
      val ESTIMATED_HOURS: String = "estimated_hours"
      val CATEGORY: String = "category_id"
    }
  }

  object Redmine {

    val ISSUE_GET_LIMIT: Int = 20

    val PATH = R2B_ROOT + "/redmine"
    val PROJECTS = PATH + "/projects.json"
    val CUSTOM_FIELDS = PATH + "/custom_fields.json"
    val PRIORITY = PATH + "/priorities.json"
    val GROUP_USERS = PATH + "/group_users.json"
    val ISSUE_STATUSES = PATH + "/issue_statuses.json"
    val TRACKERS = PATH + "/trackers.json"
    val USERS = PATH + "/users.json"

    def getIssuesDir(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/issues"
    }

    def getIssuePath(projectIdentifier: String, issueId: Int): String = {
      getIssuesDir(projectIdentifier) + s"/$issueId/$ISSUE_FILE_NAME"
    }

    def getIssueAttachmentDir(projectIdentifier: String, issueId: Int, attachmentId: Int): String = {
      getIssuesDir(projectIdentifier) + s"/$issueId/attachment/$attachmentId"
    }

    def getMembershipsPath(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/memberships.json"
    }

    def getIssueCategoriesPath(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/issue_categories.json"
    }

    def getVersionsPath(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/versions.json"
    }

    def getNewsPath(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/news.json"
    }

    def getWikisDir(projectIdentifier: String): String = {
      PATH + s"/projects/$projectIdentifier/wikis"
    }

    def getWikiPath(projectIdentifier: String, wikiTitle: String): String = {
      getWikisDir(projectIdentifier) + s"/$wikiTitle/$WIKI_FILE_NAME"
    }

    def getWikiAttachmentDir(projectIdentifier: String, wikiTitle: String, attachmentId: Int): String = {
      getWikisDir(projectIdentifier) + s"/$wikiTitle/attachment/$attachmentId"
    }

  }

}

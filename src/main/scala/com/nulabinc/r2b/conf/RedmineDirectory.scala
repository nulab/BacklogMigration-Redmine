package com.nulabinc.r2b.conf

import java.io.File

/**
  * @author uchida
  */
class RedmineDirectory(projectKey: String) {

  val workingDirectory = new File(".").getAbsoluteFile.getParent

  val ROOT = workingDirectory + "/redmine"


  val DEFAULT_PASSWORD: String = "password"

  val PROJECTS = ROOT + "/projects.json"
  val CUSTOM_FIELDS = ROOT + "/custom_fields.json"
  val PRIORITY = ROOT + "/priorities.json"
  val GROUP_USERS = ROOT + "/group_users.json"
  val ISSUE_STATUSES = ROOT + "/issue_statuses.json"
  val TRACKERS = ROOT + "/trackers.json"
  val USERS = ROOT + "/users.json"

  val ISSUE_FILE_NAME: String = "issue.json"
  val WIKI_FILE_NAME: String = "wiki.json"

  def getMembershipsPath(): String =
    s"${ROOT}/projects/$projectKey/memberships.json"

  def getIssueCategoriesPath(): String =
    s"${ROOT}/projects/$projectKey/issue_categories.json"

  def getVersionsPath(): String =
    s"${ROOT}/projects/$projectKey/versions.json"

  def getIssuesDir(): String =
    s"${ROOT}/projects/$projectKey/issues"

  def getIssuePath(issueId: Int): String =
    s"${getIssuesDir()}/$issueId/$ISSUE_FILE_NAME"

  def getIssueAttachmentDir(issueId: Int, attachmentId: Int): String =
    s"${getIssuesDir()}/$issueId/attachment/$attachmentId"

  def getWikisDir(): String =
    s"${ROOT}/projects/$projectKey/wikis"

  def getWikiPath(wikiTitle: String): String =
    s"${getWikisDir()}/$wikiTitle/$WIKI_FILE_NAME"

  def getWikiAttachmentDir(wikiTitle: String, attachmentId: Int): String =
    s"${getWikisDir()}/$wikiTitle/attachment/$attachmentId"

  def getNewsPath(): String =
    s"${ROOT}/projects/$projectKey/news.json"

}

object RedmineDirectory {

  val workingDirectory = new File(".").getAbsoluteFile.getParent

  val ROOT = workingDirectory + "/redmine"

  val USER_MAPPING_FILE = workingDirectory + "/mapping/users.json"
  val STATUS_MAPPING_FILE = workingDirectory + "/mapping/statuses.json"
  val PRIORITY_MAPPING_FILE = workingDirectory + "/mapping/priorities.json"

}
package com.nulabinc.backlog.r2b.core

import com.nulabinc.backlog.migration.common.utils.Logging
import com.osinka.i18n.Messages

object MessageResources extends Logging {

  val priorityMappingItemName: String = Messages("common.priorities")
  val statusMappingItemName: String   = Messages("common.statuses")
  val userMappingItemName: String     = Messages("common.users")

  val deleted: String = Messages("common.deleted")
  val confirm: String = Messages("cli.confirm")

  val cancel: String =
    s"""
       |--------------------------------------------------
       |${Messages("cli.cancel")}""".stripMargin

  val helpMessage: String =
    s"""
       |${Messages("cli.help.sample_command")}
       |${Messages("cli.help")}
    """.stripMargin

  def projectAlreadyExists(projectKey: String): String =
    Messages("cli.backlog_project_already_exist", projectKey)

  def validationError(errors: Seq[String]): String =
    s"""
       |
       |${Messages("cli.param.error")}
       |--------------------------------------------------
       |${errors.mkString("\n")}
       |
     """.stripMargin

  def changeCommentDoneRatio(originalValue: String, newValue: String): String =
    Messages("common.change_comment", Messages("common.done_ratio"), originalValue, newValue)

  def changeCommentRelation(originalValue: String, newValue: String): String =
    Messages("common.change_comment", Messages("common.relation"), originalValue, newValue)

  def changeCommentPrivate(originalValue: String, newValue: String): String =
    Messages("common.change_comment", Messages("common.private"), originalValue, newValue)

  def changeCommentProject(originalValue: String, newValue: String): String =
    Messages("common.change_comment", Messages("common.project"), originalValue, newValue)

  def changeCommentParentIssue(originalValue: String, newValue: String): String =
    Messages("common.change_comment", Messages("common.parent_issue"), originalValue, newValue)
}

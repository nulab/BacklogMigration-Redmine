package com.nulabinc.backlog.r2b.core

import com.nulabinc.backlog.migration.common.utils.Logging
import com.osinka.i18n.Messages

object MessageResources extends Logging {

  val deleted: String = Messages("common.deleted")

  def changeCommentDoneRatio(originalValue: String, newValue: String): String = Messages("common.change_comment", Messages("common.done_ratio"), originalValue, newValue)

  def changeCommentRelation(originalValue: String, newValue: String): String = Messages("common.change_comment", Messages("common.relation"), originalValue, newValue)

  def changeCommentPrivate(originalValue: String, newValue: String): String = Messages("common.change_comment", Messages("common.private"), originalValue, newValue)

  def changeCommentProject(originalValue: String, newValue: String): String = Messages("common.change_comment", Messages("common.project"), originalValue, newValue)

  def changeCommentParentIssue(originalValue: String, newValue: String): String = Messages("common.change_comment", Messages("common.parent_issue"), originalValue, newValue)
}

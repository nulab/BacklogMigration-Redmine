package com.nulabinc.r2b.exporter.convert

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.domain.{BacklogChangeLog, BacklogComment}
import com.nulabinc.backlog4j.Issue.StatusType
import com.nulabinc.backlog4j.Status

import scala.collection.mutable

/**
  * @author uchida
  */
class StatusConverter(backlogStatuses: Seq[Status]) {

  def convert(comments: Seq[BacklogComment]): Seq[BacklogComment] = {
    val list: mutable.ArrayBuffer[BacklogComment] = mutable.ArrayBuffer()

    comments.foreach(comment => list += comment)
    list.flatMap(convertComment)
  }

  def convertComment(comment: BacklogComment): Option[BacklogComment] = {
    val newChangeLogs = comment.changeLogs.flatMap(convertChangeLog)
    if (newChangeLogs.isEmpty && comment.optContent.isEmpty) None
    else Some(comment.copy(changeLogs = newChangeLogs))
  }

  def convertChangeLog(changeLog: BacklogChangeLog): Option[BacklogChangeLog] = {

    (changeLog.field, changeLog.optOriginalValue, changeLog.optNewValue) match {
      case (BacklogConstantValue.ChangeLog.STATUS, Some(originalValue), Some(newValue)) =>
        if (originalValue == newValue) None
        else if (is(originalValue, StatusType.Closed) && !is(newValue, StatusType.InProgress)) None
        else Some(changeLog)
      case _ => Some(changeLog)
    }
  }

  private[this] def is(name: String, statusType: StatusType): Boolean = {
    toStatus(name) match {
      case Some(status) => status.getStatusType == statusType
      case _            => false
    }
  }

  private[this] def toStatus(name: String): Option[Status] = {
    backlogStatuses.find(status => status.getName == name)
  }

}

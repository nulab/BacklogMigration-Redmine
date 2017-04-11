package com.nulabinc.r2b.redmine.domain

import com.nulabinc.backlog.migration.utils.StringUtil
import com.taskadapter.redmineapi.bean.{IssuePriority, User, Version}

/**
  * @author uchida
  */
case class PropertyValue(users: Seq[User], versions: Seq[Version], priorities: Seq[IssuePriority]) {

  def versionOfId(value: String): Option[Version] =
    StringUtil.safeStringToInt(value) match {
      case Some(intValue) => versions.find(version => version.getId == intValue)
      case _              => None
    }

  def priorityOfId(value: String): Option[IssuePriority] =
    StringUtil.safeStringToInt(value) match {
      case Some(intValue) => priorities.find(priority => priority.getId == intValue)
      case _              => None
    }

}

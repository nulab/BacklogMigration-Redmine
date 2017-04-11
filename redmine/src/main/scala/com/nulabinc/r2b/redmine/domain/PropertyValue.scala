package com.nulabinc.r2b.redmine.domain

import com.nulabinc.backlog.migration.utils.StringUtil
import com.taskadapter.redmineapi.bean.{User, Version}

/**
  * @author uchida
  */
case class PropertyValue(versions: Seq[Version], users: Seq[User]) {

  def versionOfId(value: String): Option[Version] =
    StringUtil.safeStringToInt(value) match {
      case Some(intValue) => versions.find(version => version.getId == intValue)
      case _              => None
    }

}

package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogVersion
import com.nulabinc.backlog.migration.utils.DateUtil
import com.taskadapter.redmineapi.bean.Version

/**
  * @author uchida
  */
class VersionsWrites @Inject()() extends Writes[Seq[Version], Seq[BacklogVersion]] {

  override def writes(versions: Seq[Version]): Seq[BacklogVersion] = {
    versions.map(toBacklog)
  }

  private[this] def toBacklog(version: Version) = {
    BacklogVersion(
      optId = Some(version.getId.intValue()),
      name = version.getName,
      description = Option(version.getDescription).getOrElse(""),
      optStartDate = None,
      optReleaseDueDate = Option(version.getDueDate).map(DateUtil.dateFormat),
      delete = false
    )
  }

}

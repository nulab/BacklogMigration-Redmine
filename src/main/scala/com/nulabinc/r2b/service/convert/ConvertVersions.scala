package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.importer.domain.{BacklogVersion, BacklogVersionsWrapper}
import com.nulabinc.r2b.domain.RedmineVersion

/**
  * @author uchida
  */
object ConvertVersions {

  def apply(versions: Seq[RedmineVersion]): BacklogVersionsWrapper = {
    val backlogVersions: Seq[BacklogVersion] = versions.map(getBacklogVersion)
    BacklogVersionsWrapper(backlogVersions)
  }

  private def getBacklogVersion(redmineVersion: RedmineVersion): BacklogVersion =
    BacklogVersion(
      name = redmineVersion.name,
      description = redmineVersion.description,
      startDate = None,
      releaseDueDate = redmineVersion.dueDate)

}
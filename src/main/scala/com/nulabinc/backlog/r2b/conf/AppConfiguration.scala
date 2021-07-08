package com.nulabinc.backlog.r2b.conf

import com.nulabinc.backlog.migration.common.conf.{
  BacklogApiConfiguration,
  ExcludeOption
}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration

case class AppConfiguration(
    redmineConfig: RedmineApiConfiguration,
    backlogConfig: BacklogApiConfiguration,
    exclude: ExcludeOption,
    importOnly: Boolean,
    retryCount: Int
)

case class DestroyConfiguration(
    backlogConfig: BacklogApiConfiguration,
    dryRun: Boolean
)

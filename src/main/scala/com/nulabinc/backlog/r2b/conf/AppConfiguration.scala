package com.nulabinc.backlog.r2b.conf

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration

case class ExcludeOption(excludeIssue: Boolean, excludeWiki: Boolean)

object ExcludeOption {
  val default: ExcludeOption = ExcludeOption(excludeIssue = false, excludeWiki = false)
}

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
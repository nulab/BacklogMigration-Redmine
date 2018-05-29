package com.nulabinc.backlog.r2b.conf

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration

/**
  * @author uchida
  */
case class AppConfiguration(
  redmineConfig: RedmineApiConfiguration,
  backlogConfig: BacklogApiConfiguration,
  exclude: Option[List[String]],
  importOnly: Boolean
)

case class DestroyConfiguration(
  backlogConfig: BacklogApiConfiguration,
  dryRun: Boolean
)
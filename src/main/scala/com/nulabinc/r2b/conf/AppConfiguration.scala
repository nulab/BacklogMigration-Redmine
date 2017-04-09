package com.nulabinc.r2b.conf

import com.nulabinc.backlog.migration.conf.BacklogApiConfiguration
import com.nulabinc.r2b.redmine.conf.RedmineConfig

/**
  * @author uchida
  */
case class AppConfiguration(redmineConfig: RedmineConfig, backlogConfig: BacklogApiConfiguration, importOnly: Boolean)

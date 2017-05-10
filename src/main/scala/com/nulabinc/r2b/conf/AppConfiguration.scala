package com.nulabinc.r2b.conf

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration

/**
  * @author uchida
  */
case class AppConfiguration(redmineConfig: RedmineApiConfiguration, backlogConfig: BacklogApiConfiguration, importOnly: Boolean, optOut: Boolean)

package com.nulabinc.r2b.conf

import com.nulabinc.backlog.migration.conf.BacklogConfig

/**
  * @author uchida
  */
case class AppConfiguration(
                             redmineConfig: RedmineConfig,
                             backlogConfig: BacklogConfig,
                             projectKeyMap: ProjectKeyMap,
                             importOnly: Boolean)

case class RedmineConfig(url: String, key: String)
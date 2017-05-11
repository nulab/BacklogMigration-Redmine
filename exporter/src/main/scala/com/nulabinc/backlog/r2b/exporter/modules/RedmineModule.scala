package com.nulabinc.backlog.r2b.exporter.modules

import com.google.inject.name.Names
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.RedmineDefaultModule

/**
  * @author uchida
  */
private[exporter] class RedmineModule(apiConfig: RedmineApiConfiguration, backlogProjectKey: String) extends RedmineDefaultModule(apiConfig) {

  override def configure() = {
    super.configure()
    bind(classOf[BacklogPaths]).toInstance(new BacklogPaths(backlogProjectKey))
    bind(classOf[String]).annotatedWith(Names.named("backlogProjectKey")).toInstance(backlogProjectKey)
  }

}

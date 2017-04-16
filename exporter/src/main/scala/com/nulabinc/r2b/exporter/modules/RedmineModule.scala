package com.nulabinc.r2b.exporter.modules

import com.google.inject.name.Names
import com.nulabinc.backlog.migration.conf.{BacklogApiConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.service.{StatusService, StatusServiceImpl}
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory}
import com.nulabinc.backlog4j.conf.BacklogPackageConfigure
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.r2b.redmine.modules.RedmineDefaultModule

/**
  * @author uchida
  */
class RedmineModule(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration)
    extends RedmineDefaultModule(redmineApiConfig) {

  val backlog = createBacklogClient()

  override def configure() = {
    super.configure()
    bind(classOf[BacklogClient]).toInstance(backlog)
    bind(classOf[BacklogPaths]).toInstance(new BacklogPaths(backlogApiConfig.projectKey))
    bind(classOf[String]).annotatedWith(Names.named("backlogProjectKey")).toInstance(backlogApiConfig.projectKey)
    bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
  }

  private[this] def createBacklogClient(): BacklogClient = {
    val backlogPackageConfigure = new BacklogPackageConfigure(backlogApiConfig.url)
    val configure               = backlogPackageConfigure.apiKey(backlogApiConfig.key)
    new BacklogClientFactory(configure).newClient()
  }

}

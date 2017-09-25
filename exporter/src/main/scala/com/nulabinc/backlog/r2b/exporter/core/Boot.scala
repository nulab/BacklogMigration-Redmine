package com.nulabinc.backlog.r2b.exporter.core

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.modules.{AkkaModule, ConfigModule}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.exporter.modules.{ActorModule, RedmineModule}
import com.nulabinc.backlog.r2b.exporter.service.ProjectExporter
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
object Boot extends Logging {

  def execute(apiConfig: RedmineApiConfiguration, mappingContainer: MappingContainer, backlogProjectKey: String) = {

    val injector = Guice.createInjector(
      new RedmineModule(apiConfig, mappingContainer, backlogProjectKey),
      new ConfigModule(),
      new AkkaModule(),
      new ActorModule()
    )

    ConsoleOut.println(s"""
                          |${Messages("export.start")}
                          |--------------------------------------------------""".stripMargin)

    val projectExporter = injector.getInstance(classOf[ProjectExporter])
    projectExporter.boot(injector, mappingContainer)

    ConsoleOut.println(s"""--------------------------------------------------
                          |${Messages("export.finish")}""".stripMargin)

  }

}

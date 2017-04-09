package com.nulabinc.r2b.exporter.controllers

import com.google.inject.Guice
import com.nulabinc.backlog.migration.modules.{AkkaModule, ConfigModule}
import com.nulabinc.backlog.migration.utils.{ConsoleOut, Logging}
import com.nulabinc.r2b.exporter.modules.{ActorModule, RedmineModule}
import com.nulabinc.r2b.exporter.service.ProjectApplicationService
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
object ExportController extends Logging {

  def execute(apiConfig: RedmineConfig, backlogProjectKey: String) = {

    val injector = Guice.createInjector(
      new RedmineModule(apiConfig, backlogProjectKey),
      new ConfigModule(),
      new AkkaModule(),
      new ActorModule()
    )

    ConsoleOut.println(s"""
                          |${Messages("export.start")}
                          |--------------------------------------------------""".stripMargin)

    val service = injector.getInstance(classOf[ProjectApplicationService])
    service.execute(injector)

    ConsoleOut.println(s"""--------------------------------------------------
                          |${Messages("export.finish")}""".stripMargin)

  }

}

package com.nulabinc.backlog.r2b.exporter.core

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.exporter.conf.ExportConfig
import com.nulabinc.backlog.r2b.exporter.modules.RedmineModule
import com.nulabinc.backlog.r2b.exporter.service.ProjectExporter
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
object Boot extends Logging {

  def execute(apiConfig: RedmineApiConfiguration, mappingContainer: MappingContainer, backlogProjectKey: BacklogProjectKey, backlogTextFormattingRule: BacklogTextFormattingRule, exclude: Option[List[String]]) = {
    try {
      val injector = Guice.createInjector(new RedmineModule(apiConfig, mappingContainer, backlogProjectKey, backlogTextFormattingRule, ExportConfig(exclude)))

      ConsoleOut.println(s"""
                            |${Messages("export.start")}
                            |--------------------------------------------------""".stripMargin)

      val projectExporter = injector.getInstance(classOf[ProjectExporter])
      projectExporter.boot(mappingContainer)

      ConsoleOut.println(s"""--------------------------------------------------
                            |${Messages("export.finish")}""".stripMargin)
    } catch {
      case e: Throwable =>
        ConsoleOut.error(s"${Messages("cli.error.unknown")}:${e.getMessage}")
        throw e
    }
  }

}

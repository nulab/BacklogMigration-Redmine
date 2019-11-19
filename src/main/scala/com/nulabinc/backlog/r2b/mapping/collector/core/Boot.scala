package com.nulabinc.backlog.r2b.mapping.collector.core

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.conf.ExcludeOption
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.mapping.collector.modules.RedmineModule
import com.nulabinc.backlog.r2b.mapping.collector.service.MappingCollector
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable

/**
  * @author uchida
  */
object Boot extends Logging {

  def execute(apiConfig: RedmineApiConfiguration, exclude: ExcludeOption): MappingData = {
    try {
      val injector = Guice.createInjector(new RedmineModule(apiConfig))

      ConsoleOut.println(s"""
                            |${Messages("cli.project_info.start")}
                            |--------------------------------------------------""".stripMargin)

      val mappingData      = MappingData(mutable.Set.empty[User], mutable.Set.empty[String])
      val mappingCollector = injector.getInstance(classOf[MappingCollector])
      mappingCollector.boot(exclude, mappingData)

      ConsoleOut.println(s"""|--------------------------------------------------
                             |${Messages("cli.project_info.finish")}""".stripMargin)

      mappingData
    } catch {
      case e: Throwable =>
        ConsoleOut.error(s"${Messages("cli.error.unknown")}:${e.getMessage}")
        throw e
    }
  }

}

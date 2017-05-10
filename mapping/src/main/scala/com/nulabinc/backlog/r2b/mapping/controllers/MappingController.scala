package com.nulabinc.r2b.controllers

import com.google.inject.Guice
import com.nulabinc.backlog.migration.common.modules.{AkkaModule, ConfigModule}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.mapping.core.MappingData
import com.nulabinc.backlog.r2b.mapping.service.ProjectApplicationService
import com.nulabinc.backlog.r2b.mapping.modules.{ActorModule, RedmineModule}
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable

/**
  * @author uchida
  */
object MappingController extends Logging {

  def execute(apiConfig: RedmineApiConfiguration): MappingData = {

    val injector = Guice.createInjector(
      new RedmineModule(apiConfig),
      new ConfigModule(),
      new AkkaModule(),
      new ActorModule()
    )

    ConsoleOut.println(s"""
                          |${Messages("cli.project_info.start")}
                          |--------------------------------------------------""".stripMargin)

    val mappingData = MappingData(mutable.Set.empty[User], mutable.Set.empty[String])
    val service     = injector.getInstance(classOf[ProjectApplicationService])
    service.execute(injector, mappingData)

    ConsoleOut.println(s"""|--------------------------------------------------
                           |${Messages("cli.project_info.finish")}""".stripMargin)

    mappingData
  }

}

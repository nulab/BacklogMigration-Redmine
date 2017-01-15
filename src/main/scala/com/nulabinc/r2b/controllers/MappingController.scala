package com.nulabinc.r2b.controllers

import com.google.inject.Guice
import com.nulabinc.backlog.migration.di.{AkkaModule, ConfigModule}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.actor.mapping.di.ActorModule
import com.nulabinc.r2b.actor.mapping.service.ProjectApplicationService
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.di.RedmineModule
import com.nulabinc.r2b.mapping.MappingData
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable


/**
  * @author uchida
  */
object MappingController extends Logging {

  def execute(config: AppConfiguration): MappingData = {

    val injector = Guice.createInjector(
      new RedmineModule(config, Seq.empty[User]),
      new ConfigModule(),
      new AkkaModule(),
      new ActorModule()
    )

    log.info(
      s"""
         |${showMessage(LOG_Header1, Messages("cli.collect_project_info.start"))}
         |--------------------------------------------------""".stripMargin)

    val mappingData = MappingData(mutable.Set.empty[User], mutable.Set.empty[String])
    val service = injector.getInstance(classOf[ProjectApplicationService])
    service.execute(injector, mappingData)

    log.info(
      s"""|--------------------------------------------------
          |${showMessage(LOG_Header1, Messages("cli.collect_project_info.finish"))}""".stripMargin)

    mappingData
  }

}
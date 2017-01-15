package com.nulabinc.r2b.controllers

import com.google.inject.Guice
import com.nulabinc.backlog.migration.di.{AkkaModule, ConfigModule}
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.actor.redmine.di.ActorModule
import com.nulabinc.r2b.actor.redmine.service.ProjectApplicationService
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.di.RedmineModule
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
object ExportController extends Logging {

  private val logKInd = LOG_Header1

  def execute(config: AppConfiguration, needUsers: Seq[User]) = {

    val injector = Guice.createInjector(
      new RedmineModule(config, needUsers),
      new ConfigModule(),
      new AkkaModule(),
      new ActorModule()
    )

    log.info(
      s"""
         |${showMessage(logKInd, Messages("export.start_redmine_export"))}
         |--------------------------------------------------""".stripMargin)

    val service = injector.getInstance(classOf[ProjectApplicationService])
    service.execute(injector)

    log.info(
      s"""--------------------------------------------------
         |${showMessage(logKInd, Messages("export.completed_redmine_export"))}""".stripMargin)

  }

}

package com.nulabinc.r2b.controllers

import com.google.inject.Guice
import com.nulabinc.backlog.migration.di.ConfigModule
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.di.RedmineModule
import com.nulabinc.r2b.service.convert.ConvertApplicationService
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
object ConvertController {

  def execute(config: AppConfiguration, needUsers: Seq[User]) = {

    val injector = Guice.createInjector(
      new RedmineModule(config, needUsers),
      new ConfigModule()
    )

    val service = injector.getInstance(classOf[ConvertApplicationService])
    service.execute()
  }

}

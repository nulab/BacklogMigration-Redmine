package com.nulabinc.backlog.r2b.redmine.modules

import com.google.inject.{AbstractModule, Guice, Injector}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.service._
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}

/**
  * @author uchida
  */
object ServiceInjector {

  def createInjector(apiConfig: RedmineApiConfiguration): Injector = {
    Guice.createInjector(new AbstractModule() {
      override def configure(): Unit = {
        val transportConfig = RedmineManagerFactory.createShortTermConfig(RedmineManagerFactory.createInsecureConnectionManager())
        val redmine         = RedmineManagerFactory.createWithApiKey(apiConfig.url, apiConfig.key, transportConfig)

        bind(classOf[RedmineManager]).toInstance(redmine)
        bind(classOf[PriorityService]).to(classOf[PriorityServiceImpl])
        bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
        bind(classOf[UserService]).to(classOf[UserServiceImpl])
        bind(classOf[ProjectService]).to(classOf[ProjectServiceImpl])
      }
    })
  }

}

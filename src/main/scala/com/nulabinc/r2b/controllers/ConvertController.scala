package com.nulabinc.r2b.controllers
//
//import com.google.inject.Guice
//import com.nulabinc.backlog.migration.modules.ConfigModule
//import com.nulabinc.backlog.migration.utils.{ConsoleOut, Logging}
//import com.nulabinc.r2b.conf.AppConfiguration
//import com.nulabinc.r2b.di.RedmineModule
//import com.nulabinc.r2b.service.convert.ConvertApplicationService
//import com.osinka.i18n.Messages
//import com.taskadapter.redmineapi.bean.User
//
///**
//  * @author uchida
//  */
//object ConvertController extends Logging {
//
//  def execute(config: AppConfiguration, needUsers: Seq[User]) = {
//
//    val injector = Guice.createInjector(
//      new RedmineModule(config, needUsers),
//      new ConfigModule()
//    )
//
//    ConsoleOut.println(s"""
//                          |${Messages("convert.start")}
//                          |--------------------------------------------------""".stripMargin)
//
//    val service = injector.getInstance(classOf[ConvertApplicationService])
//    service.execute()
//
//    ConsoleOut.println(s"""--------------------------------------------------
//                          |${Messages("convert.finish")}""".stripMargin)
//
//  }
//
//}

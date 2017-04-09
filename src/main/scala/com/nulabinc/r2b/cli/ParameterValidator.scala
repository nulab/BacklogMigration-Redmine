package com.nulabinc.r2b.cli

import com.nulabinc.backlog.migration.modules.ServiceInjector
import com.nulabinc.backlog.migration.service.UserService
import com.nulabinc.backlog.migration.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.redmine.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.{RedmineAuthenticationException, RedmineTransportException}

/**
  * @author uchida
  */
class ParameterValidator(config: AppConfiguration) extends Logging {

  def validate(): Seq[String] = {
    val backlogErrors: Seq[String] = validateConfigBacklog()
    if (config.importOnly) backlogErrors
    else {
      val redmineErrors: Seq[String] = validateConfigRedmine()
      if (redmineErrors.nonEmpty) backlogErrors union redmineErrors
      else
        backlogErrors union
          redmineErrors union
          validateProject()
    }
  }

  private[this] def validateProject(): Seq[String] = {
    val redmineService = new RedmineService(config.redmineConfig)
    redmineService.optProject(config.redmineConfig.projectKey) match {
      case None => Seq(s"- ${Messages("cli.param.error.disable.project", config.redmineConfig.projectKey)}")
      case _    => Seq.empty[String]
    }
  }

  private[this] def validateConfigBacklog(): Seq[String] = {
    ConsoleOut.info(Messages("cli.param.check.access", Messages("common.backlog")))
    val messages = try {
      val injector    = ServiceInjector.createInjector(config.backlogConfig)
      val userService = injector.getInstance(classOf[UserService])
      userService.allUsers()
      Seq.empty[String]
    } catch {
      case unknown: BacklogAPIException if unknown.getStatusCode == 404 =>
        Seq(s"- ${Messages("cli.param.error.disable.host", Messages("common.backlog"), config.backlogConfig.url)}")
      case _: Throwable =>
        Seq(s"- ${Messages("cli.param.error.disable.access", Messages("common.backlog"))}")
    }
    messages
  }

  private[this] def validateConfigRedmine(): Seq[String] =
    try {
      val redmineService = new RedmineService(config.redmineConfig)
      redmineService.getUsers
      Seq.empty[String]
    } catch {
      case _: RedmineAuthenticationException =>
        Seq("- " + Messages("cli.param.error.auth", Messages("common.redmine")))
      case _: RedmineTransportException =>
        Seq("- " + Messages("cli.param.error.disable.host", Messages("common.redmine"), config.redmineConfig.url))
      case _: Throwable =>
        Seq("- " + Messages("cli.param.error.disable.access", Messages("common.redmine")))
    }

}

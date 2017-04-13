package com.nulabinc.r2b.cli

import com.nulabinc.backlog.migration.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.service.{UserService => BacklogUserService}
import com.nulabinc.backlog.migration.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.r2b.redmine.service.{ProjectService, UserService => RedmineUserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.Project
import com.taskadapter.redmineapi.{RedmineAuthenticationException, RedmineTransportException}

/**
  * @author uchida
  */
class ParameterValidator(config: AppConfiguration) extends Logging {

  def validate(): Seq[String] = {
    val redmineErrors: Seq[String] = validateConfigRedmine()
    val backlogErrors: Seq[String] = validateConfigBacklog()
    ConsoleOut.info(Messages("cli.param.get.project", Messages("common.redmine")))
    val optRedmineProject = optProject()
    if (config.importOnly) backlogErrors
    else {
      if (redmineErrors.nonEmpty) {
        backlogErrors union redmineErrors
      } else {
        backlogErrors union
          redmineErrors union
          validateProject(optRedmineProject) union
          validProjectKey(config.backlogConfig.projectKey)
      }
    }
  }

  private[this] def validateProject(optRedmineProject: Option[Project]): Seq[String] = {
    optRedmineProject match {
      case None => Seq(s"- ${Messages("cli.param.error.disable.project", config.redmineConfig.projectKey)}")
      case _    => Seq.empty[String]
    }
  }

  private[this] def validateConfigBacklog(): Seq[String] = {
    ConsoleOut.info(Messages("cli.param.check.access", Messages("common.backlog")))
    val messages = try {
      val injector    = BacklogInjector.createInjector(config.backlogConfig)
      val userService = injector.getInstance(classOf[BacklogUserService])
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

  private[this] def validateConfigRedmine(): Seq[String] = {
    ConsoleOut.info(Messages("cli.param.check.access", Messages("common.redmine")))
    try {
      val injector    = RedmineInjector.createInjector(config.redmineConfig)
      val userService = injector.getInstance(classOf[RedmineUserService])
      userService.allUsers()
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

  private[this] def validProjectKey(projectKey: String): Seq[String] = {
    if (projectKey.matches("""^[0-9A-Z_]+$"""))
      Seq.empty[String]
    else
      Seq(s"- ${Messages("cli.param.error.project_key", projectKey)}")
  }

  private[this] def optProject(): Option[Project] = {
    val injector       = RedmineInjector.createInjector(config.redmineConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    projectService.optProjectOfKey(config.redmineConfig.projectKey)
  }

}

package com.nulabinc.r2b.cli

import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.SpaceService
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{ProjectService, UserService => RedmineUserService}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.r2b.conf.AppConfiguration
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.Project
import com.taskadapter.redmineapi.{NotAuthorizedException, RedmineAuthenticationException, RedmineTransportException}

/**
  * @author uchida
  */
class ParameterValidator(config: AppConfiguration) extends Logging {

  def validate(): Seq[String] = {
    val validateRedmine = validateConfigRedmine()
    val validateBacklog = validateConfigBacklog()
    ConsoleOut.info(Messages("cli.param.get.project", Messages("common.redmine")))
    val optRedmineProject = optProject()

    val messages = Seq(validateBacklog, validProjectKey(config.backlogConfig.projectKey), validateAuthBacklog(validateBacklog)).flatten

    if (config.importOnly) {
      messages
    } else {
      messages union Seq(validateRedmine, validateProject(optRedmineProject)).flatten
    }
  }

  private[this] def validateProject(optRedmineProject: Option[Project]): Option[String] = {
    optRedmineProject match {
      case None => Some(s"- ${Messages("cli.param.error.disable.project", config.redmineConfig.projectKey)}")
      case _    => None
    }
  }

  private[this] def validateConfigBacklog(): Option[String] = {
    ConsoleOut.info(Messages("cli.param.check.access", Messages("common.backlog")))
    val messages = try {
      val injector     = BacklogInjector.createInjector(config.backlogConfig)
      val spaceService = injector.getInstance(classOf[SpaceService])
      spaceService.space()
      None
    } catch {
      case unknown: BacklogAPIException if unknown.getStatusCode == 404 =>
        logger.error(unknown.getMessage, unknown)
        Some(s"- ${Messages("cli.param.error.disable.host", Messages("common.backlog"), config.backlogConfig.url)}")
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Some(s"- ${Messages("cli.param.error.disable.access", Messages("common.backlog"))}")
    }
    messages
  }

  private[this] def validateAuthBacklog(resultValidateConfig: Option[String]): Option[String] = {
    if (resultValidateConfig.isEmpty) {
      ConsoleOut.info(Messages("cli.param.check.admin"))
      val injector     = BacklogInjector.createInjector(config.backlogConfig)
      val spaceService = injector.getInstance(classOf[SpaceService])
      if (spaceService.hasAdmin()) None
      else Some(s"- ${Messages("cli.param.error.auth.backlog")}")
    } else None
  }

  private[this] def validateConfigRedmine(): Option[String] = {
    ConsoleOut.info(Messages("cli.param.check.access", Messages("common.redmine")))
    try {
      val injector    = RedmineInjector.createInjector(config.redmineConfig)
      val userService = injector.getInstance(classOf[RedmineUserService])
      userService.allUsers()
      None
    } catch {
      case auth: RedmineAuthenticationException =>
        logger.error(auth.getMessage, auth)
        Some(s"- ${Messages("cli.param.error.auth", Messages("common.redmine"))}")
      case noauth: NotAuthorizedException =>
        logger.error(noauth.getMessage, noauth)
        Some(s"- ${Messages("cli.param.error.auth.not.auth", noauth.getMessage)}")
      case transport: RedmineTransportException =>
        logger.error(transport.getMessage, transport)
        Some(s"- ${Messages("cli.param.error.disable.host", Messages("common.redmine"), config.redmineConfig.url)}")
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Some(s"- ${Messages("cli.param.error.disable.access", Messages("common.redmine"))}")
    }
  }

  private[this] def validProjectKey(projectKey: String): Option[String] = {
    if (projectKey.matches("""^[0-9A-Z_]+$""")) None
    else Some(s"- ${Messages("cli.param.error.project_key", projectKey)}")
  }

  private[this] def optProject(): Option[Project] = {
    val injector       = RedmineInjector.createInjector(config.redmineConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    projectService.optProjectOfKey(config.redmineConfig.projectKey)
  }

}

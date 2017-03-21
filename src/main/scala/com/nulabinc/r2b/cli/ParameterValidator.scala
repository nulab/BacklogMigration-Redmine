package com.nulabinc.r2b.cli

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.service.{BacklogService, RedmineService}
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
    val redmineService: RedmineService = new RedmineService(config.redmineConfig)
    redmineService.optProject(config.projectKeyMap.redmine) match {
      case None => Seq(s"- ${Messages("cli.can_not_load_project", config.projectKeyMap.redmine)}")
      case _ => Seq.empty[String]
    }
  }

  private[this] def validateConfigBacklog(): Seq[String] =
    try {
      val backlogService = new BacklogService(config.backlogConfig)
      backlogService.users
      Seq.empty[String]
    } catch {
      case unknown: BacklogAPIException if unknown.getStatusCode == 404 =>
        log.error(unknown.getMessage, unknown)
        Seq(s"- ${Messages("cli.transport_error_backlog", config.backlogConfig.url)}")
      case e: Throwable =>
        log.error(e.getMessage, e)
        Seq(s"- ${Messages("cli.disable_access_backlog")}")
    }

  private[this] def validateConfigRedmine(): Seq[String] =
    try {
      val redmineService = new RedmineService(config.redmineConfig)
      redmineService.getUsers
      Seq.empty[String]
    } catch {
      case auth: RedmineAuthenticationException =>
        log.error(auth)
        Seq("- " + Messages("cli.auth_error_redmine"))
      case transport: RedmineTransportException =>
        log.error(transport)
        Seq("- " + Messages("cli.transport_error_redmine", config.redmineConfig.url))
      case e: Throwable =>
        log.error(e)
        Seq("- " + Messages("cli.disable_access_redmine"))
    }

}

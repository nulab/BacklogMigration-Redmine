package com.nulabinc.r2b.cli

import com.google.inject.Injector
import com.nulabinc.backlog.importer.controllers.ImportController
import com.nulabinc.backlog.migration.conf.{BacklogConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.modules.ServiceInjector
import com.nulabinc.backlog.migration.service.{ProjectService, SpaceService, UserService}
import com.nulabinc.backlog.migration.utils.{ConsoleOut, Logging, MixpanelUtil, TrackingData}
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.controllers.MappingController
import com.nulabinc.r2b.exporter.controllers.ExportController
import com.nulabinc.r2b.mapping.core._
import com.osinka.i18n.Messages

import scala.util.Try

/**
  * @author uchida
  */
object R2BCli extends BacklogConfiguration with Logging {

  def init(config: AppConfiguration): Unit =
    if (validateParam(config)) {
      val propertyMappingFiles = createMapping(config)
      output(propertyMappingFiles.user)
      output(propertyMappingFiles.status)
      output(propertyMappingFiles.priority)
    }

  def migrate(config: AppConfiguration): Unit =
    if (validateParam(config)) {
      if (config.importOnly) ImportController.execute(config.backlogConfig, false)
      else {
        val propertyMappingFiles = createMapping(config)
        if (validateMapping(propertyMappingFiles.user) &&
            validateMapping(propertyMappingFiles.status) &&
            validateMapping(propertyMappingFiles.priority)) {
          if (confirmImport(config, propertyMappingFiles)) {

            val backlogInjector = ServiceInjector.createInjector(config.backlogConfig)
            val backlogPaths    = backlogInjector.getInstance(classOf[BacklogPaths])
            backlogPaths.outputPath.deleteRecursively(force = true, continueOnFailure = true)

            ExportController.execute(config.redmineConfig, config.backlogConfig.projectKey)
            ImportController.execute(config.backlogConfig, false)
            tracking(config, backlogInjector)
          }
        }
      }
    }

  def doImport(config: AppConfiguration): Unit =
    if (validateParam(config)) {
      ImportController.execute(config.backlogConfig, false)
      val backlogInjector = ServiceInjector.createInjector(config.backlogConfig)
      tracking(config, backlogInjector)
    }

  private[this] def tracking(config: AppConfiguration, backlogInjector: Injector) = {
    Try {
      val space       = backlogInjector.getInstance(classOf[SpaceService]).space()
      val myself      = backlogInjector.getInstance(classOf[UserService]).myself()
      val environment = backlogInjector.getInstance(classOf[SpaceService]).environment()
      val data = TrackingData(product = mixpanelProduct,
                              envname = environment.name,
                              spaceId = environment.spaceId,
                              userId = myself.id,
                              srcUrl = config.redmineConfig.url,
                              dstUrl = config.backlogConfig.url,
                              srcProjectKey = config.redmineConfig.projectKey,
                              dstProjectKey = config.backlogConfig.projectKey,
                              srcSpaceCreated = "",
                              dstSpaceCreated = space.created)
      MixpanelUtil.track(token = mixpanelToken, data = data)
    }
  }

  private[this] def confirmRecreate(mappingFile: MappingFile): Boolean = {
    val input: String = scala.io.StdIn.readLine(Messages("cli.confirm_recreate", mappingFile.itemName, mappingFile.filePath))
    input == "y" || input == "Y"
  }

  private[this] def validateParam(config: AppConfiguration): Boolean = {
    val validator           = new ParameterValidator(config)
    val errors: Seq[String] = validator.validate()
    if (errors.isEmpty) true
    else {
      val message =
        s"""
           |
          |${Messages("cli.param.error")}
           |--------------------------------------------------
           |${errors.mkString("\n")}
           |
        """.stripMargin
      ConsoleOut.error(message)
      false
    }
  }

  private[this] def validateMapping(mappingFile: MappingFile): Boolean = {
    if (!mappingFile.isExists) {
      ConsoleOut.error(s"""
           |--------------------------------------------------
           |${Messages("cli.invalid_setup")}""".stripMargin)
      false
    } else if (!mappingFile.isParsed) {
      val error =
        s"""
           |--------------------------------------------------
           |${Messages("cli.mapping.error.broken_file", mappingFile.itemName)}
           |--------------------------------------------------
        """.stripMargin
      ConsoleOut.error(error)
      val message =
        s"""|--------------------------------------------------
            |${Messages("cli.mapping.fix_file", mappingFile.filePath)}""".stripMargin
      ConsoleOut.println(message)
      false
    } else if (!mappingFile.isValid) {
      val error =
        s"""
           |${Messages("cli.mapping.error", mappingFile.itemName)}
           |--------------------------------------------------
           |${mappingFile.errors.mkString("\n")}
           |--------------------------------------------------""".stripMargin
      ConsoleOut.error(error)
      val message =
        s"""
           |--------------------------------------------------
           |${Messages("cli.mapping.fix_file", mappingFile.filePath)}
        """.stripMargin
      ConsoleOut.println(message)
      false
    } else true
  }

  private[this] def confirmImport(config: AppConfiguration, propertyMappingFiles: PropertyMappingFiles): Boolean = {
    confirmProject(config) match {
      case Some(projectKeys) =>
        val (redmine, backlog): (String, String) = projectKeys
        ConsoleOut.println(s"""
                              |${Messages("cli.mapping.show", Messages("common.projects"))}
                              |--------------------------------------------------
                              |- ${redmine} => ${backlog}
                              |--------------------------------------------------
                              |
         |${Messages("cli.mapping.show", propertyMappingFiles.user.itemName)}
                              |--------------------------------------------------
                              |${mappingString(propertyMappingFiles.user)}
                              |--------------------------------------------------
                              |
         |${Messages("cli.mapping.show", propertyMappingFiles.priority.itemName)}
                              |--------------------------------------------------
                              |${mappingString(propertyMappingFiles.priority)}
                              |--------------------------------------------------
                              |
         |${Messages("cli.mapping.show", propertyMappingFiles.status.itemName)}
                              |--------------------------------------------------
                              |${mappingString(propertyMappingFiles.status)}
                              |--------------------------------------------------
                              |""".stripMargin)
        val input: String = scala.io.StdIn.readLine(Messages("cli.confirm"))
        if (input == "y" || input == "Y") true
        else {
          ConsoleOut.println(s"""
                                |--------------------------------------------------
                                |${Messages("cli.cancel")}""".stripMargin)
          false
        }
      case _ =>
        ConsoleOut.println(s"""
                              |--------------------------------------------------
                              |${Messages("cli.cancel")}""".stripMargin)
        false
    }
  }

  private[this] def mappingString(mappingFile: MappingFile): String =
    mappingFile.unmarshal() match {
      case Some(mappings) =>
        mappings
          .map(mapping =>
            s"- ${mappingFile.display(mapping.redmine, mappingFile.redmines)} => ${mappingFile.display(mapping.backlog, mappingFile.backlogs)}")
          .mkString("\n")
      case _ => throw new RuntimeException
    }

  private[this] def confirmProject(config: AppConfiguration): Option[(String, String)] = {
    val injector       = ServiceInjector.createInjector(config.backlogConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    val optProject     = projectService.optProject(config.backlogConfig.projectKey)
    optProject match {
      case Some(_) =>
        val input: String = scala.io.StdIn.readLine(Messages("cli.backlog_project_already_exist", config.backlogConfig.projectKey))
        if (input == "y" || input == "Y") Some((config.redmineConfig.projectKey, config.backlogConfig.projectKey))
        else None
      case None =>
        Some((config.redmineConfig.projectKey, config.backlogConfig.projectKey))
    }
  }

  private[this] def createMapping(config: AppConfiguration): PropertyMappingFiles = {
    val mappingData     = MappingController.execute(config.redmineConfig)
    val userMapping     = new UserMappingFile(config.redmineConfig, config.backlogConfig, mappingData)
    val statusMapping   = new StatusMappingFile(config.redmineConfig, config.backlogConfig, mappingData)
    val priorityMapping = new PriorityMappingFile(config.redmineConfig, config.backlogConfig)
    PropertyMappingFiles(user = userMapping, status = statusMapping, priority = priorityMapping)
  }

  private[this] def output(mappingFile: MappingFile) = {
    if (mappingFile.isExists) {
      if (confirmRecreate(mappingFile)) {
        mappingFile.create()
        val message =
          s"""${Messages("cli.mapping.output_file", mappingFile.itemName, mappingFile.filePath)}
            |
            |${Messages("cli.confirm.fix")}
          """.stripMargin
        ConsoleOut.info(message)
      }
    } else {
      mappingFile.create()
      val message =
        s"""${Messages("cli.mapping.output_file", mappingFile.itemName, mappingFile.filePath)}
          |
          |${Messages("cli.confirm.fix")}
        """.stripMargin
      ConsoleOut.info(message)
    }
  }

  def help() = {
    val message =
      s"""
         |${Messages("cli.help.sample_command")}
         |${Messages("cli.help")}
      """.stripMargin
    ConsoleOut.println(message)
  }

}

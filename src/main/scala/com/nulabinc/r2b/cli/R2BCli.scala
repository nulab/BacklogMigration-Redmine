package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.conf.ImportConfig
import com.nulabinc.backlog.importer.controllers.ImportController
import com.nulabinc.backlog.migration.conf.BacklogDirectory
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.conf.{AppConfiguration, ProjectKeyMap, RedmineDirectory}
import com.nulabinc.r2b.controllers.{ConvertController, ExportController, MappingController}
import com.nulabinc.r2b.mapping._
import com.nulabinc.r2b.service.BacklogService
import com.osinka.i18n.Messages
import com.typesafe.config.ConfigFactory

import scalax.file.Path

/**
  * @author uchida
  */
object R2BCli extends Logging {

  def init(config: AppConfiguration) =
    if (validateParam(config)) {
      val propertyMappingFiles = createMapping(config)
      output(propertyMappingFiles.user)
      output(propertyMappingFiles.status)
      output(propertyMappingFiles.priority)
    }

  def migrate(config: AppConfiguration) =
    if (validateParam(config)) {
      if (config.importOnly) ImportController.execute(getImportConfig(config))
      else {
        val propertyMappingFiles = createMapping(config)
        if (validateMapping(config, propertyMappingFiles.user) &&
          validateMapping(config, propertyMappingFiles.status) &&
          validateMapping(config, propertyMappingFiles.priority)) {
          if (confirmImport(config, propertyMappingFiles)) {
            ExportController.execute(config, propertyMappingFiles.user.getNeedUsers())
            ConvertController.execute(config, propertyMappingFiles.user.getNeedUsers())
            ImportController.execute(getImportConfig(config))
          }
        }
      }
    }

  def doImport(config: AppConfiguration) =
    if (validateParam(config)) ImportController.execute(getImportConfig(config))

  def help() =
    log.info(
      s"""
         |${ConfigFactory.load().getString("application.title")}
         |--------------------------------------------------
         |${Messages("cli.help.sample_command")}
         |${Messages("cli.help")}""".stripMargin)

  private[this] def confirmRecreate(mappingFile: MappingFile): Boolean = {
    val input: String = scala.io.StdIn.readLine(Messages("cli.confirm_recreate", mappingFile.itemName, mappingFile.filePath))
    input == "y" || input == "Y"
  }

  private[this] def validateParam(config: AppConfiguration): Boolean = {
    val validator = new ParameterValidator(config)
    val errors: Seq[String] = validator.validate()
    log.info(
      s"""|${ConfigFactory.load().getString("application.title")}
          |--------------------------------------------------""".stripMargin)
    if (errors.isEmpty) true
    else {
      log.info(
        s"""${Messages("mapping.show_parameter_error")}
           |--------------------------------------------------
           |${errors.mkString("\n")}
           |--------------------------------------------------""".stripMargin)
      false
    }
  }

  private[this] def validateMapping(config: AppConfiguration, mappingFile: MappingFile): Boolean = {
    Path.fromString(RedmineDirectory.ROOT).deleteRecursively(force = true, continueOnFailure = true)
    Path.fromString(BacklogDirectory.ROOT).deleteRecursively(force = true, continueOnFailure = true)
    if (!mappingFile.isExists) {
      log.info(
        s"""
           |--------------------------------------------------
           |${Messages("cli.invalid_setup")}""".stripMargin)
      false
    } else if (!mappingFile.isParsed) {
      log.info(
        s"""
           |--------------------------------------------------
           |${Messages("mapping.broken_file", mappingFile.itemName)}
           |${Messages("mapping.need_fix_file", mappingFile.filePath)}""".stripMargin)
      false
    } else if (!mappingFile.isValid) {
      log.info(
        s"""
           |${Messages("mapping.show_error", mappingFile.itemName)}
           |--------------------------------------------------
           |${mappingFile.errors.mkString("\n")}
           |--------------------------------------------------
           |
          |--------------------------------------------------
           |${Messages("mapping.need_fix_file", mappingFile.filePath)}""".stripMargin)
      false
    } else true
  }

  private[this] def confirmImport(config: AppConfiguration, propertyMappingFiles: PropertyMappingFiles): Boolean = {
    val backlogService = new BacklogService(config.backlogConfig)
    val optProjectKeyMap: Option[ProjectKeyMap] = confirmProject(backlogService, config.projectKeyMap)
    log.info(
      s"""
         |${Messages("mapping.show", Messages("common.projects"))}
         |--------------------------------------------------
         |- ${config.projectKeyMap.redmine} => ${config.projectKeyMap.getBacklogKey()}
         |--------------------------------------------------
         |
         |${Messages("mapping.show", propertyMappingFiles.user.itemName)}
         |--------------------------------------------------
         |${mappingString(propertyMappingFiles.user)}
         |--------------------------------------------------
         |
         |${Messages("mapping.show", propertyMappingFiles.priority.itemName)}
         |--------------------------------------------------
         |${mappingString(propertyMappingFiles.priority)}
         |--------------------------------------------------
         |
         |${Messages("mapping.show", propertyMappingFiles.status.itemName)}
         |--------------------------------------------------
         |${mappingString(propertyMappingFiles.status)}
         |--------------------------------------------------
         |""".stripMargin)
    val input: String = scala.io.StdIn.readLine(Messages("mapping.confirm"))
    if (optProjectKeyMap.isDefined && input == "y" || input == "Y") true
    else {
      log.info(
        s"""
           |--------------------------------------------------
           |${Messages("cli.cancel")}""".stripMargin)
      false
    }
  }

  private[this] def mappingString(mappingFile: MappingFile): String = {
    val either: Either[Throwable, Seq[Mapping]] = mappingFile.unmarshal().right.map(_.mappings)
    val mappings: Seq[Mapping] = either.right.get
    mappings.map(mapping =>
      s"- ${mappingFile.display(mapping.redmine, mappingFile.redmines)} => ${mappingFile.display(mapping.backlog, mappingFile.backlogs)}"
    ).mkString("\n")
  }

  private[this] def confirmProject(backlogService: BacklogService, projectKeyMap: ProjectKeyMap): Option[ProjectKeyMap] =
    backlogService.optProject(projectKeyMap.getBacklogKey()) match {
      case Some(_) =>
        val input: String = scala.io.StdIn.readLine(Messages("cli.backlog_project_already_exist", projectKeyMap.getBacklogKey()))
        if (input == "y" || input == "Y") Some(projectKeyMap)
        else None
      case None =>
        Some(projectKeyMap)
    }

  private[this] def getImportConfig(config: AppConfiguration) =
    ImportConfig(
      url = config.backlogConfig.url,
      key = config.backlogConfig.key,
      projectKey = config.projectKeyMap.getBacklogKey())

  private[this] def createMapping(config: AppConfiguration): PropertyMappingFiles = {
    val mappingData = MappingController.execute(config)
    val userMapping = new UserMappingFile(config, mappingData)
    val statusMapping = new StatusMappingFile(config, mappingData)
    val priorityMapping = new PriorityMappingFile(config)
    PropertyMappingFiles(user = userMapping,
      status = statusMapping,
      priority = priorityMapping)
  }

  private[this] def output(mappingFile: MappingFile) = {
    if (mappingFile.isExists) {
      if (confirmRecreate(mappingFile)) {
        mappingFile.create()
        log.info(
          s"""${Messages("cli.output_mapping_file", mappingFile.itemName, mappingFile.filePath)}
             |
               |${Messages("cli.confirm_fix")}""".stripMargin)
      }
    } else {
      mappingFile.create()
      log.info(
        s"""${Messages("cli.output_mapping_file", mappingFile.itemName, mappingFile.filePath)}
           |
             |${Messages("cli.confirm_fix")}""".stripMargin)
    }
  }

}

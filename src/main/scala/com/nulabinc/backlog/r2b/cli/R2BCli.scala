package com.nulabinc.backlog.r2b.cli

import java.net.{HttpURLConnection, URL}

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration, BacklogPaths, MappingDirectory}
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.interpreters.{JansiConsoleDSL, LocalStorageDSL}
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{
  PriorityService => BacklogPriorityService,
  ProjectService,
  StatusService => BacklogStatusService
}
import com.nulabinc.backlog.migration.common.services.{PriorityMappingFileService, StatusMappingFileService}
import com.nulabinc.backlog.migration.common.utils.ControlUtil.using
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.migration.importer.core.{Boot => BootImporter}
import com.nulabinc.backlog.r2b.conf.AppConfiguration
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem}
import com.nulabinc.backlog.r2b.exporter.core.{Boot => BootExporter}
import com.nulabinc.backlog.r2b.mapping.collector.core.{Boot => BootMapping}
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.domain.Mapping
import com.nulabinc.backlog.r2b.mapping.file._
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler

/**
  * @author uchida
  */
object R2BCli extends BacklogConfiguration with Logging {
  import com.nulabinc.backlog.r2b.deserializers.RedmineMappingDeserializer._
  import com.nulabinc.backlog.r2b.formatters.RedmineFormatter._
  import com.nulabinc.backlog.r2b.mapping.RedmineMappingHeader._
  import com.nulabinc.backlog.r2b.serializers.RedmineMappingSerializer._

  private implicit val exc: Scheduler               = monix.execution.Scheduler.Implicits.global
  private implicit val storageDSL: StorageDSL[Task] = LocalStorageDSL()
  private implicit val consoleDSL: ConsoleDSL[Task] = JansiConsoleDSL()

  def init(config: AppConfiguration): Unit = {
    val injector               = BacklogInjector.createInjector(config.backlogConfig)
    val backlogStatusService   = injector.getInstance(classOf[BacklogStatusService])
    val backlogPriorityService = injector.getInstance(classOf[BacklogPriorityService])

    if (validateParam(config)) {
      val mappingFileContainer = createMapping(config)
//      output(mappingFileContainer.user)
//      output(mappingFileContainer.priority)

      StatusMappingFileService
        .init[RedmineStatusMappingItem, Task](
          mappingFilePath = MappingDirectory.default.statusMappingFilePath,
          mappingListPath = MappingDirectory.default.statusMappingListFilePath,
          srcItems = mappingFileContainer.status.redmines.map(r => RedmineStatusMappingItem(r.name)),
          dstItems = backlogStatusService.allStatuses()
        )
        .runSyncUnsafe()

      PriorityMappingFileService
        .init[RedminePriorityMappingItem, Task](
          mappingFilePath = MappingDirectory.default.priorityMappingFilePath,
          mappingListPath = MappingDirectory.default.priorityMappingListFilePath,
          srcItems = mappingFileContainer.priority.redmines.map(r => RedminePriorityMappingItem(r.name)),
          dstItems = backlogPriorityService.allPriorities()
        )
        .runSyncUnsafe()
    }
  }

  def migrate(config: AppConfiguration): Unit = {
    val retryCount = config.retryCount

    if (validateParam(config)) {
      if (config.importOnly) BootImporter.execute(config.backlogConfig, fitIssueKey = false, retryCount = retryCount)
      else {
        val mappingFileContainer = createMapping(config)
        if (
          true // TODO
//          validateMapping(mappingFileContainer.user) &&
//          validateMapping(mappingFileContainer.status) && // TODO: fix
//          validateMapping(mappingFileContainer.priority)
        ) {
          if (confirmImport(config, mappingFileContainer)) {

            val backlogInjector        = BacklogInjector.createInjector(config.backlogConfig)
            val backlogPaths           = backlogInjector.getInstance(classOf[BacklogPaths])
            val backlogStatusService   = backlogInjector.getInstance(classOf[BacklogStatusService])
            val backlogPriorityService = backlogInjector.getInstance(classOf[BacklogPriorityService])

            if (backlogPaths.outputPath.exists) {
              backlogPaths.outputPath.listRecursively.foreach(_.delete(false))
            }

            for {
              statusMappings <-
                StatusMappingFileService
                  .execute[RedmineStatusMappingItem, Task](
                    path = MappingDirectory.default.statusMappingFilePath,
                    dstItems = backlogStatusService.allStatuses()
                  )
                  .runSyncUnsafe()
              priorityMappings <-
                PriorityMappingFileService
                  .execute[RedminePriorityMappingItem, Task](
                    path = MappingDirectory.default.priorityMappingFilePath,
                    dstItems = backlogPriorityService.allPriorities()
                  )
                  .runSyncUnsafe()
            } yield {
              val mappingContainer = MappingContainer(
                user = mappingFileContainer.user.tryUnmarshal(),
                statuses = statusMappings,
                priority = priorityMappings
              )
              val backlogTextFormattingRule = fetchBacklogTextFormattingRule(config.backlogConfig)

              BootExporter.execute(
                config.redmineConfig,
                mappingContainer,
                BacklogProjectKey(config.backlogConfig.projectKey),
                backlogTextFormattingRule,
                config.exclude
              )
              BootImporter.execute(config.backlogConfig, fitIssueKey = false, retryCount = retryCount)
              finalize(config.backlogConfig)
            }
          }
        }
      }
    }
  }

  def doImport(config: AppConfiguration): Unit = {
    if (validateParam(config)) {
      BootImporter.execute(config.backlogConfig, fitIssueKey = false, retryCount = config.retryCount)
      finalize(config.backlogConfig)
    }
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

  private[this] def confirmProject(config: AppConfiguration): Option[(String, String)] = {
    val injector       = BacklogInjector.createInjector(config.backlogConfig)
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

  private[this] def confirmImport(config: AppConfiguration, mappingFileContainer: MappingFileContainer): Boolean = {
    confirmProject(config) match {
      case Some(projectKeys) =>
        val (redmine, backlog): (String, String) = projectKeys
//        ConsoleOut.println(s"""
//                              |${Messages("cli.mapping.show", Messages("common.projects"))}
//                              |--------------------------------------------------
//                              |- ${redmine} => ${backlog}
//                              |--------------------------------------------------
//                              |
//                              |${Messages("cli.mapping.show", mappingFileContainer.user.itemName)}
//                              |--------------------------------------------------
//                              |${mappingString(mappingFileContainer.user)}
//                              |--------------------------------------------------
//                              |""".stripMargin)
//        if (mappingFileContainer.priority.nonEmpty()) {
//          ConsoleOut.println(s"""${Messages("cli.mapping.show", mappingFileContainer.priority.itemName)}
//                                |--------------------------------------------------
//                                |${mappingString(mappingFileContainer.priority)}
//                                |--------------------------------------------------""".stripMargin)
//        }
//        if (mappingFileContainer.status.nonEmpty()) { // TODO: fix
//          ConsoleOut.println(s"""${Messages("cli.mapping.show", mappingFileContainer.status.itemName)}
//                                |--------------------------------------------------
//                                |${mappingString(mappingFileContainer.status)}
//                                |--------------------------------------------------""".stripMargin)
//        }
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

  private[this] def mappingString(mappingFile: MappingFile): String = {
    mappingFile.unmarshal() match {
      case Some(mappings) =>
        mappings
          .map(mapping =>
            s"- ${mappingFile.display(mapping.redmine, mappingFile.redmines)} => ${mappingFile.display(mapping.backlog, mappingFile.backlogs)}"
          )
          .mkString("\n")
      case _ => throw new RuntimeException
    }
  }

  private[this] def createMapping(config: AppConfiguration): MappingFileContainer = {
    val mappingData     = BootMapping.execute(config.redmineConfig, config.exclude)
    val userMapping     = new UserMappingFile(config.redmineConfig, config.backlogConfig, mappingData.users.toSeq)
    val statusMapping   = new StatusMappingFile(config.redmineConfig, config.backlogConfig, mappingData.statuses.toSeq)
    val priorityMapping = new PriorityMappingFile(config.redmineConfig, config.backlogConfig)
    MappingFileContainer(user = userMapping, status = statusMapping, priority = priorityMapping)
  }

  private[this] def output(mappingFile: MappingFile) = {
    if (mappingFile.isExists) {
      val addItems = mappingFile.merge()
      val message = if (addItems.nonEmpty) {
        def displayItem(value: String) = {
          if (value.isEmpty) Messages("common.empty") else value
        }
        def display(mapping: Mapping) = {
          s"- ${mapping.redmine} => ${displayItem(mapping.backlog)}"
        }
        val mappingString = addItems.map(display).mkString("\n")
        s"""
           |--------------------------------------------------
           |${Messages("cli.mapping.merge_file", mappingFile.itemName, mappingFile.filePath)}
           |[${mappingFile.filePath}]
           |${mappingString}
           |--------------------------------------------------""".stripMargin
      } else {
        s"""
           |--------------------------------------------------
           |${Messages("cli.mapping.no_change", mappingFile.itemName)}
           |--------------------------------------------------""".stripMargin
      }
      ConsoleOut.println(message)
    } else {
      def afterMessage(): Unit = {
        val message =
          s"""
             |--------------------------------------------------
             |${Messages("cli.mapping.output_file", mappingFile.itemName)}
             |[${mappingFile.filePath}]
             |--------------------------------------------------""".stripMargin
        ConsoleOut.println(message)
        ()
      }
      mappingFile.create(afterMessage _)
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

  private[this] def finalize(config: BacklogApiConfiguration) = {
    if (!versionName.contains("SNAPSHOT")) {
      val url = new URL(s"${config.url}/api/v2/importer/redmine?projectKey=${config.projectKey}")
      url.openConnection match {
        case http: HttpURLConnection =>
          http.setRequestMethod("GET")
          http.connect()
          using(http) { connection =>
            connection.getResponseCode
          }
        case _ => 0
      }
    }
  }

  private[this] def fetchBacklogTextFormattingRule(backlogConfig: BacklogApiConfiguration): BacklogTextFormattingRule = {
    val injector       = BacklogInjector.createInjector(backlogConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    val optProject     = projectService.optProject(backlogConfig.projectKey)
    optProject match {
      case Some(project) => BacklogTextFormattingRule(project.textFormattingRule)
      case _             => BacklogTextFormattingRule("markdown")
    }
  }

}

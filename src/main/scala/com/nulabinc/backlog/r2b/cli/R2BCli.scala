package com.nulabinc.backlog.r2b.cli

import java.net.{HttpURLConnection, URL}

import cats.data.EitherT
import com.nulabinc.backlog.migration.common.conf.{
  BacklogApiConfiguration,
  BacklogConfiguration,
  BacklogPaths,
  MappingDirectory
}
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.interpreters.{JansiConsoleDSL, LocalStorageDSL}
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{
  ProjectService,
  PriorityService => BacklogPriorityService,
  StatusService => BacklogStatusService,
  UserService => BacklogUserService
}
import com.nulabinc.backlog.migration.common.services.{
  PriorityMappingFileService,
  StatusMappingFileService,
  UserMappingFileService
}
import com.nulabinc.backlog.migration.common.utils.ControlUtil.using
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.migration.importer.core.{Boot => BootImporter}
import com.nulabinc.backlog.r2b.conf.AppConfiguration
import com.nulabinc.backlog.r2b.core.MessageResources
import com.nulabinc.backlog.r2b.domain.mappings.{
  RedminePriorityMappingItem,
  RedmineStatusMappingItem,
  RedmineUserMappingItem
}
import com.nulabinc.backlog.r2b.exporter.core.{Boot => BootExporter}
import com.nulabinc.backlog.r2b.mapping.collector.core.{Boot => BootMapping}
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.file._
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.{AppError, MappingError, OperationCanceled, ValidationError}
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler

/**
  * @author uchida
  */
object R2BCli extends BacklogConfiguration with Logging {
  import com.nulabinc.backlog.migration.common.shared.syntax._
  import com.nulabinc.backlog.r2b.deserializers.RedmineMappingDeserializer._
  import com.nulabinc.backlog.r2b.formatters.RedmineFormatter._
  import com.nulabinc.backlog.r2b.mapping.RedmineMappingHeader._
  import com.nulabinc.backlog.r2b.serializers.RedmineMappingSerializer._

  private implicit val exc: Scheduler               = monix.execution.Scheduler.Implicits.global
  private implicit val storageDSL: StorageDSL[Task] = LocalStorageDSL()
  private implicit val consoleDSL: ConsoleDSL[Task] = JansiConsoleDSL()

  def init(config: AppConfiguration): Task[Either[AppError, Unit]] = {
    val injector               = BacklogInjector.createInjector(config.backlogConfig)
    val backlogStatusService   = injector.getInstance(classOf[BacklogStatusService])
    val backlogPriorityService = injector.getInstance(classOf[BacklogPriorityService])
    val backlogUserService     = injector.getInstance(classOf[BacklogUserService])

    for {
      _ <- validateParam(config)
    } yield {
      val mappingFileContainer = createMapping(config)

      StatusMappingFileService
        .init[RedmineStatusMappingItem, Task](
          mappingFilePath = MappingDirectory.default.statusMappingFilePath,
          mappingListPath = MappingDirectory.default.statusMappingListFilePath,
          srcItems =
            mappingFileContainer.status.redmines.map(r => RedmineStatusMappingItem(r.name)),
          dstItems = backlogStatusService.allStatuses()
        )
        .runSyncUnsafe()

      PriorityMappingFileService
        .init[RedminePriorityMappingItem, Task](
          mappingFilePath = MappingDirectory.default.priorityMappingFilePath,
          mappingListPath = MappingDirectory.default.priorityMappingListFilePath,
          srcItems =
            mappingFileContainer.priority.redmines.map(r => RedminePriorityMappingItem(r.name)),
          dstItems = backlogPriorityService.allPriorities()
        )
        .runSyncUnsafe()

      UserMappingFileService
        .init[RedmineUserMappingItem, Task](
          mappingFilePath = MappingDirectory.default.userMappingFilePath,
          mappingListPath = MappingDirectory.default.userMappingListFilePath,
          srcItems =
            mappingFileContainer.user.redmines.map(r => RedmineUserMappingItem(r.name, r.display)),
          dstItems = backlogUserService.allUsers(),
          dstApiConfiguration = config.backlogConfig
        )
        .runSyncUnsafe()

      Right(())
    }
  }

  def migrate(config: AppConfiguration): Task[Either[AppError, Unit]] = {
    val backlogInjector      = BacklogInjector.createInjector(config.backlogConfig)
    val backlogPaths         = backlogInjector.getInstance(classOf[BacklogPaths])
    val backlogStatusService = backlogInjector.getInstance(classOf[BacklogStatusService])
    val backlogPriorityService =
      backlogInjector.getInstance(classOf[BacklogPriorityService])
    val backlogUserService = backlogInjector.getInstance(classOf[BacklogUserService])

    val result = for {
      _ <- validateParam(config).handleError
      userMappings <-
        UserMappingFileService
          .execute[RedmineUserMappingItem, Task](
            MappingDirectory.default.userMappingFilePath,
            backlogUserService.allUsers()
          )
          .mapError(MappingError)
          .handleError
      statusMappings <-
        StatusMappingFileService
          .execute[RedmineStatusMappingItem, Task](
            MappingDirectory.default.statusMappingFilePath,
            backlogStatusService.allStatuses()
          )
          .mapError(MappingError)
          .handleError
      priorityMappings <-
        PriorityMappingFileService
          .execute[RedminePriorityMappingItem, Task](
            MappingDirectory.default.priorityMappingFilePath,
            backlogPriorityService.allPriorities()
          )
          .mapError(MappingError)
          .handleError
      mappingContainer = MappingContainer(userMappings, priorityMappings, statusMappings)
      _ <- confirmImport(config, mappingContainer).handleError
    } yield {

      if (backlogPaths.outputPath.exists) {
        backlogPaths.outputPath.listRecursively.foreach(_.delete(false))
      }

      val backlogTextFormattingRule = fetchBacklogTextFormattingRule(config.backlogConfig)

      BootExporter.execute(
        config.redmineConfig,
        mappingContainer,
        BacklogProjectKey(config.backlogConfig.projectKey),
        backlogTextFormattingRule,
        config.exclude
      )
      BootImporter.execute(
        config.backlogConfig,
        fitIssueKey = false,
        retryCount = config.retryCount
      )
      finalize(config.backlogConfig)
      ()
    }
    result.value
  }

  def doImport(config: AppConfiguration): Task[Either[AppError, Unit]] =
    for {
      _ <- validateParam(config)
    } yield {
      BootImporter
        .execute(config.backlogConfig, fitIssueKey = false, retryCount = config.retryCount)
      finalize(config.backlogConfig)
      Right(())
    }

  private def validateParam(config: AppConfiguration): Task[Either[AppError, Unit]] = {
    val validator = new ParameterValidator(config)
    val errors    = validator.validate()

    if (errors.isEmpty)
      Task(Right(()))
    else {
      for {
        _ <- consoleDSL.errorln(MessageResources.validationError(errors))
      } yield Left(ValidationError(errors))
    }
  }

  private def confirmProject(config: AppConfiguration): Task[Either[AppError, (String, String)]] = {
    val injector       = BacklogInjector.createInjector(config.backlogConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    val optProject     = projectService.optProject(config.backlogConfig.projectKey)
    val result = optProject match {
      case Some(_) =>
        for {
          input <- readProjectAlreadyExists(config.backlogConfig).handleError
          answer <-
            checkProjectAlreadyExists(input, config.redmineConfig, config.backlogConfig).handleError
        } yield answer
      case None =>
        EitherT.fromEither[Task](
          Right((config.redmineConfig.projectKey, config.backlogConfig.projectKey))
        )
    }

    result.value
  }

  private def readProjectAlreadyExists(
      config: BacklogApiConfiguration
  ): Task[Either[AppError, String]] =
    ConsoleDSL[Task].read(MessageResources.projectAlreadyExists(config.projectKey)).map(Right(_))

  private def checkProjectAlreadyExists(
      input: String,
      redmineConfig: RedmineApiConfiguration,
      backlogConfig: BacklogApiConfiguration
  ): Task[Either[AppError, (String, String)]] =
    Task {
      if (input.toLowerCase == "y") Right((redmineConfig.projectKey, backlogConfig.projectKey))
      else Left(OperationCanceled)
    }

  private def confirmImport(
      config: AppConfiguration,
      mappingContainer: MappingContainer
  ): Task[Either[AppError, Unit]] = {
    val userStr = mappingContainer.user
      .map(item => toMappingRow(item.src.displayName, item.dst.value))
      .mkString("\n")
    val priorityStr = mappingContainer.priority
      .map(item => toMappingRow(item.src.value, item.dst.value))
      .mkString("\n")
    val statusStr = mappingContainer.statuses
      .map(item => s"- ${item.src.value} => ${item.dst.value}")
      .mkString("\n")

    val result = for {
      keys <- confirmProject(config).handleError
      _    <- projectInfoMessage(keys._1, keys._2).handleError
      _    <- userMappingMessage(userStr).handleError
      _    <- priorityMappingMessage(priorityStr).handleError
      _    <- statusMappingMessage(statusStr).handleError
    } yield ()

    result.value
  }

  private def projectInfoMessage(
      redmineProjectKey: String,
      backlogProjectKey: String
  ): Task[Either[AppError, Unit]] =
    ConsoleDSL[Task]
      .println(
        s"""
           |${Messages("cli.mapping.show", Messages("common.projects"))}
           |--------------------------------------------------
           |- $redmineProjectKey => $backlogProjectKey
           |--------------------------------------------------
           |
           |""".stripMargin
      )
      .map(Right(_))

  private def userMappingMessage(str: String): Task[Either[AppError, Unit]] =
    ConsoleDSL[Task]
      .println(
        s"""${Messages("cli.mapping.show", MessageResources.userMappingItemName)}
            |--------------------------------------------------
            |$str
            |--------------------------------------------------
            |""".stripMargin
      )
      .map(Right(_))

  private def priorityMappingMessage(str: String): Task[Either[AppError, Unit]] =
    ConsoleDSL[Task]
      .println(
        s"""${Messages("cli.mapping.show", MessageResources.priorityMappingItemName)}
            |--------------------------------------------------
            |$str
            |--------------------------------------------------""".stripMargin
      )
      .map(Right(_))

  private def statusMappingMessage(str: String): Task[Either[AppError, Unit]] =
    ConsoleDSL[Task]
      .println(
        s"""${Messages("cli.mapping.show", MessageResources.statusMappingItemName)}
            |--------------------------------------------------
            |$str
            |--------------------------------------------------""".stripMargin
      )
      .map(Right(_))

  private def toMappingRow(src: String, dst: String): String =
    s"- $src => $dst"

  private def createMapping(config: AppConfiguration): MappingFileContainer = {
    val mappingData = BootMapping.execute(config.redmineConfig, config.exclude)
    val userMapping =
      new UserMappingFile(config.redmineConfig, config.backlogConfig, mappingData.users.toSeq)
    val statusMapping =
      new StatusMappingFile(config.redmineConfig, config.backlogConfig, mappingData.statuses.toSeq)
    val priorityMapping = new PriorityMappingFile(config.redmineConfig, config.backlogConfig)
    MappingFileContainer(user = userMapping, status = statusMapping, priority = priorityMapping)
  }

  def help(): Task[Either[AppError, Unit]] =
    consoleDSL.println(MessageResources.helpMessage).map(Right(_))

  private def finalize(config: BacklogApiConfiguration) = {
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

  private def fetchBacklogTextFormattingRule(
      backlogConfig: BacklogApiConfiguration
  ): BacklogTextFormattingRule = {
    val injector       = BacklogInjector.createInjector(backlogConfig)
    val projectService = injector.getInstance(classOf[ProjectService])
    val optProject     = projectService.optProject(backlogConfig.projectKey)
    optProject match {
      case Some(project) => BacklogTextFormattingRule(project.textFormattingRule)
      case _             => BacklogTextFormattingRule("markdown")
    }
  }
}

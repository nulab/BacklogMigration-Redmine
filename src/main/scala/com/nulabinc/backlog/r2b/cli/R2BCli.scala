package com.nulabinc.backlog.r2b.cli

import java.net.{HttpURLConnection, URL}

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogTextFormattingRule}
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.ProjectService
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.migration.importer.core.{Boot => BootImporter}
import com.nulabinc.backlog.r2b.conf.{AppConfiguration, DestroyConfiguration}
import com.nulabinc.backlog.r2b.dsl.BacklogDSL
import com.nulabinc.backlog.r2b.exporter.core.{Boot => BootExporter}
import com.nulabinc.backlog.r2b.interpreters.AppDSL.AppProgram
import com.nulabinc.backlog.r2b.interpreters._
import com.nulabinc.backlog.r2b.interpreters.backlog.Backlog4jInterpreter
import com.nulabinc.backlog.r2b.mapping.collector.core.{Boot => BootMapping}
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.domain.Mapping
import com.nulabinc.backlog.r2b.mapping.file._
import com.nulabinc.backlog4j.Issue
import com.osinka.i18n.Messages
import monix.execution.Scheduler
import com.nulabinc.backlog.migration.common.utils.ControlUtil.using

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object FutureUtils {
  case class Suspend[A](eval: () => Future[A])

  def sequential[A](prgs: Seq[Suspend[A]])(implicit exc: ExecutionContext): Future[Seq[A]] = {
    prgs.foldLeft(Future.successful(Seq.empty[A])) {
      case (acc, future) =>
        acc.flatMap(res => future.eval().map(res2 => res :+ res2))
    }
  }

}

/**
  * @author uchida
  */
object R2BCli extends BacklogConfiguration with Logging {

  def init(config: AppConfiguration): Unit = {
    if (validateParam(config)) {
      val mappingFileContainer = createMapping(config)
      output(mappingFileContainer.user)
      output(mappingFileContainer.status)
      output(mappingFileContainer.priority)
    }
  }

  def migrate(config: AppConfiguration): Unit = {
    if (validateParam(config)) {
      if (config.importOnly) BootImporter.execute(config.backlogConfig, false)
      else {
        val mappingFileContainer = createMapping(config)
        if (validateMapping(mappingFileContainer.user) &&
          validateMapping(mappingFileContainer.status) &&
          validateMapping(mappingFileContainer.priority)) {
          if (confirmImport(config, mappingFileContainer)) {

            val backlogInjector = BacklogInjector.createInjector(config.backlogConfig)
            val backlogPaths    = backlogInjector.getInstance(classOf[BacklogPaths])

            if (backlogPaths.outputPath.exists) {
              backlogPaths.outputPath.listRecursively.foreach(_.delete(false))
            }

            val mappingContainer = MappingContainer(user = mappingFileContainer.user.tryUnmarshal(),
              status = mappingFileContainer.status.tryUnmarshal(),
              priority = mappingFileContainer.priority.tryUnmarshal())


            val backlogTextFormattingRule = fetchBacklogTextFormattingRule(config.backlogConfig)

            BootExporter.execute(config.redmineConfig, mappingContainer, BacklogProjectKey(config.backlogConfig.projectKey), backlogTextFormattingRule, config.exclude)
            BootImporter.execute(config.backlogConfig, false)
            finalize(config.backlogConfig)
          }
        }
      }
    }
  }

  def doImport(config: AppConfiguration): Unit = {
    if (validateParam(config)) {
      BootImporter.execute(config.backlogConfig, false)
      finalize(config.backlogConfig)
    }
  }

  def sequence[A](prgs: Seq[AppProgram[A]]): AppProgram[Seq[A]] = {
    import com.nulabinc.backlog.r2b.interpreters.AppDSL._

    prgs.foldLeft(pure(Seq.empty[A])) {
      case (newPrg, prg) =>
        newPrg.flatMap { results =>
          prg.map { result =>
            results :+ result
          }
        }
    }
  }

  def destroy(config: DestroyConfiguration): Unit = {

    import com.nulabinc.backlog.r2b.interpreters.AppDSL._

    implicit val exc: Scheduler = monix.execution.Scheduler.Implicits.global

    val CHUNK_ISSUE_COUNT = 10

    val interpreter = AppInterpreter(
      new Backlog4jInterpreter(
        config.backlogConfig.url,
        config.backlogConfig.key
      ),
      new ConsoleInterpreter
    )

    type IssueStreamF[A] = (Seq[Issue], Int, Int) => AppProgram[A]

    def streamIssue[A](projectId: Long, limit: Int, druRun: Boolean)(f: IssueStreamF[A]): AppProgram[A] = {
      def go(current: Int): AppProgram[A] = {
        backlog(BacklogDSL.getProjectIssues(projectId, current, limit)).flatMap {
          case Right(issues) =>
            if (issues.isEmpty)
              f(issues, current, limit)
            else {
              val nextCurrent = if (druRun) current + limit else 0
              f(issues, current, limit).flatMap(_ => go(nextCurrent))
            }
          case Left(error) =>
            throw new RuntimeException(error.toString)
        }
      }
      go(0)
    }

    val program = for {
      _ <- console(ConsoleDSL.print(s"""--------------------------------------------------
                                       |${Messages("common.backlog")} ${Messages("common.url")}[${config.backlogConfig.url}]
                                       |${Messages("common.backlog")} ${Messages("common.access_key")}[${config.backlogConfig.key}]
                                       |${Messages("common.backlog")} ${Messages("common.project_key")}[${config.backlogConfig.projectKey}]
                                       |Dry run mode [${config.dryRun}]
                                       |--------------------------------------------------""".stripMargin))
      // access check
      projectResult <- for {
        accessCheck <- backlog(BacklogDSL.getProject(config.backlogConfig.projectKey))
        _ <- accessCheck match {
            case Right(_) => console(ConsoleDSL.print(Messages("cli.param.ok.access", Messages("common.backlog"))))
            case Left(error) => exit(error.toString, 1)
        }
      } yield accessCheck
      // confirm
      _ <- for {
        projectKey <- console(ConsoleDSL.read(Messages("destroy.confirm")))
        isValid = projectKey == config.backlogConfig.projectKey
        _ <- if (isValid) {
          AppDSL.pure(())
        } else {
          exit(Messages("destroy.confirm.fail"), 1)
        }
      } yield isValid
      // start
      _ <- if (config.dryRun) {
        console(ConsoleDSL.print(Messages("destroy.start.dryRun")))
      } else {
        console(ConsoleDSL.print(Messages("destroy.start")))
      }
      stream <- streamIssue(projectResult.right.get.getId, CHUNK_ISSUE_COUNT, config.dryRun) { (issues, _, _) =>
        val r = issues.map { issue =>
          for {
            _ <- pure(logger.debug("Issue Id: " + issue.getId))
            result <- if (config.dryRun) {
              backlog(BacklogDSL.pure(Right(issue)))
            } else {
              backlog(BacklogDSL.deleteIssue(issue))
            }
            _ <- result match {
              case Right(_) => console(ConsoleDSL.print(Messages("destroy.issue.deleted", issue.getIssueKey, issue.getSummary)))
              case Left(error) => console(ConsoleDSL.print(s"ERROR: ${issue.getIssueKey} ${issue.getSummary} ${error.toString}"))
            }
          } yield ()
        }
        sequence(r)
      }
    } yield stream

    val f = interpreter.run(program).runAsync

    Await.result(f, Duration.Inf)

    if (config.dryRun) {
      ConsoleOut.println(Messages("destroy.finish.dryRun"))
    } else {
      ConsoleOut.println(Messages("destroy.finish"))
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
        ConsoleOut.println(s"""
                              |${Messages("cli.mapping.show", Messages("common.projects"))}
                              |--------------------------------------------------
                              |- ${redmine} => ${backlog}
                              |--------------------------------------------------
                              |
                              |${Messages("cli.mapping.show", mappingFileContainer.user.itemName)}
                              |--------------------------------------------------
                              |${mappingString(mappingFileContainer.user)}
                              |--------------------------------------------------
                              |""".stripMargin)
        if (mappingFileContainer.priority.nonEmpty) {
          ConsoleOut.println(s"""${Messages("cli.mapping.show", mappingFileContainer.priority.itemName)}
                                |--------------------------------------------------
                                |${mappingString(mappingFileContainer.priority)}
                                |--------------------------------------------------""".stripMargin)
        }
        if (mappingFileContainer.status.nonEmpty) {
          ConsoleOut.println(s"""${Messages("cli.mapping.show", mappingFileContainer.status.itemName)}
                                |--------------------------------------------------
                                |${mappingString(mappingFileContainer.status)}
                                |--------------------------------------------------""".stripMargin)
        }
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
            s"- ${mappingFile.display(mapping.redmine, mappingFile.redmines)} => ${mappingFile.display(mapping.backlog, mappingFile.backlogs)}")
          .mkString("\n")
      case _ => throw new RuntimeException
    }
  }

  private[this] def createMapping(config: AppConfiguration): MappingFileContainer = {
    val mappingData     = BootMapping.execute(config.redmineConfig)
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
      case _ => BacklogTextFormattingRule("markdown")
    }
  }

}
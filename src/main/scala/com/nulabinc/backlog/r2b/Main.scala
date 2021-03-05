package com.nulabinc.backlog.r2b

import java.nio.file.Paths
import java.util.Locale
import akka.actor.ActorSystem
import com.nulabinc.backlog.migration.common.client.IAAH
import com.nulabinc.backlog.migration.common.conf.{
  BacklogApiConfiguration,
  BacklogConfiguration,
  ExcludeOption
}
import com.nulabinc.backlog.migration.common.errors.{MappingFileNotFound, MappingValidationError}
import com.nulabinc.backlog.migration.common.interpreters.{
  AkkaHttpDSL,
  JansiConsoleDSL,
  LocalStorageDSL,
  SQLiteStoreDSL
}
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.services.GitHubReleaseCheckService
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.cli.R2BCli
import com.nulabinc.backlog.r2b.conf._
import com.nulabinc.backlog.r2b.messages.RedmineMessages
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler
import org.fusesource.jansi.AnsiConsole
import org.rogach.scallop._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object R2B extends BacklogConfiguration with Logging {

  private val dbPath = Paths.get("./backlog/data.db")

  private final val iaahStr = ""
  private final val iaah    = IAAH(iaahStr)

  private implicit val system: ActorSystem  = ActorSystem("main")
  private implicit val exc: Scheduler       = monix.execution.Scheduler.Implicits.global
  private implicit val storageDSL           = LocalStorageDSL()
  private implicit val consoleDSL           = JansiConsoleDSL()
  private implicit val storeDSL             = SQLiteStoreDSL(dbPath)
  private implicit val httpDSL: AkkaHttpDSL = new AkkaHttpDSL()

  def main(args: Array[String]): Unit = {
    consoleDSL
      .println(s"""|${applicationName}
                 |--------------------------------------------------""".stripMargin)
      .runSyncUnsafe()
    AnsiConsole.systemInstall()
    setLang()
    DisableSSLCertificateCheckUtil.disableChecks()
    if (!ClassVersion.isValid()) {
      consoleDSL
        .errorln(
          Messages("cli.require_java8", System.getProperty("java.specification.version"))
        )
        .runSyncUnsafe()
      AnsiConsole.systemUninstall()
      System.exit(1)
    }

    try {
      val cli = new CommandLineInterface(args.toIndexedSeq)
      val asyncResult = for {
        _      <- checkRelease()
        result <- execute(cli)
        _ <- result match {
          case Right(_) =>
            Task(Right(()))
          case Left(error: ValidationError) =>
            consoleDSL.errorln(RedmineMessages.validationError(error.errors))
          case Left(error: MappingError) =>
            error.inner match {
              case _: MappingFileNotFound =>
                consoleDSL.errorln(ConsoleMessages.Mappings.needsSetup)
              case e: MappingValidationError[_] =>
                consoleDSL.errorln(ConsoleMessages.Mappings.validationError(e))
              case e =>
                consoleDSL.errorln(e.toString)
            }
          case Left(UnknownError(error)) =>
            consoleDSL.errorln(error.getStackTrace.mkString("\n"))
          case Left(OperationCanceled) =>
            consoleDSL.errorln(RedmineMessages.cancel)
        }
      } yield ()

      val f = asyncResult
        .flatMap { _ =>
          Task.deferFuture(system.terminate()).map(_ => ())
        }
        .onErrorRecover { ex =>
          logger.error(ex.getMessage, ex)
          exit(1, ex)
        }
        .runToFuture

      Await.result(f, Duration.Inf)
      AnsiConsole.systemUninstall()
      System.exit(0)
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        AnsiConsole.systemUninstall()
        Await.result(system.terminate(), Duration.Inf)
        System.exit(1)
    }
  }

  private def exit(exitCode: Int): Unit =
    System.exit(exitCode)

  private def exit(exitCode: Int, error: Throwable): Unit = {
    consoleDSL
      .errorln(
        "ERROR: " + error.getMessage + "\n" + error.printStackTrace()
      )
      .runSyncUnsafe()
    exit(exitCode)
  }

  private def execute(cli: CommandLineInterface): Task[Either[AppError, Unit]] =
    for {
      conf <- getConfiguration(cli)
      result <- cli.subcommand match {
        case Some(cli.execute) if cli.execute.importOnly() =>
          R2BCli.doImport(conf)
        case Some(cli.execute) =>
          R2BCli.migrate(conf)
        case Some(cli.init) =>
          R2BCli.init(conf)
        case _ =>
          R2BCli.help()
      }
    } yield result

  private def getConfiguration(cli: CommandLineInterface): Task[AppConfiguration] = {
    val keys    = cli.execute.projectKey().split(":")
    val redmine = keys(0)
    val backlog = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

    val retryCount = cli.execute.retryCount.toOption.getOrElse(20)
    val exclude = cli.execute.exclude.toOption
      .map { args =>
        ExcludeOption(
          issue = args.contains("issue"),
          wiki = args.contains("wiki")
        )
      }
      .getOrElse(ExcludeOption.default)

    consoleDSL
      .println(s"""--------------------------------------------------
     |${Messages("common.src")} ${Messages("common.url")}[${cli.execute.redmineUrl()}]
     |${Messages("common.src")} ${Messages("common.access_key")}[${cli.execute.redmineKey()}]
     |${Messages("common.src")} ${Messages("common.project_key")}[${redmine}]
     |${Messages("common.dst")} ${Messages("common.url")}[${cli.execute.backlogUrl()}]
     |${Messages("common.dst")} ${Messages("common.access_key")}[${cli.execute.backlogKey()}]
     |${Messages("common.dst")} ${Messages("common.project_key")}[${backlog}]
     |${Messages("common.importOnly")}[${cli.execute.importOnly()}]
     |${Messages("common.retryCount")}[$retryCount]
     |exclude[${exclude.toString}]
     |https.proxyHost[${Option(System.getProperty("https.proxyHost")).getOrElse("")}]
     |https.proxyPort[${Option(System.getProperty("https.proxyPort")).getOrElse("")}]
     |https.proxyUser[${Option(System.getProperty("https.proxyUser")).getOrElse("")}]
     |https.proxyPassword[${Option(System.getProperty("https.proxyPassword")).getOrElse("")}]
     |--------------------------------------------------
     |""".stripMargin)
      .map { _ =>
        AppConfiguration(
          redmineConfig = RedmineApiConfiguration(
            url = cli.execute.redmineUrl(),
            key = cli.execute.redmineKey(),
            projectKey = redmine
          ),
          backlogConfig = BacklogApiConfiguration(
            url = cli.execute.backlogUrl(),
            key = cli.execute.backlogKey(),
            projectKey = backlog,
            iaah = cli.execute.iaah.getOrElse(iaah)
          ),
          exclude = exclude,
          importOnly = cli.execute.importOnly(),
          retryCount = retryCount
        )
      }
  }

  private[this] def setLang(): Unit =
    if (language == "ja") Locale.setDefault(Locale.JAPAN)
    else Locale.setDefault(Locale.US)

  private def checkRelease(): Task[Unit] =
    GitHubReleaseCheckService.check[Task](
      path = "https://api.github.com/repos/nulab/BacklogMigration-Redmine/releases",
      currentVersion = versionName
    )
}

class CommandLineInterface(arguments: Seq[String])
    extends ScallopConf(arguments)
    with BacklogConfiguration
    with Logging {

  banner("""Usage: Backlog Migration for Redmine [OPTION]....
           | """.stripMargin)
  footer("\n " + Messages("cli.help"))

  val help    = opt[String]("help", descr = Messages("cli.help.show_help"))
  val version = opt[String]("version", descr = Messages("cli.help.show_version"))

  val execute = new Subcommand("execute") {
    val backlogKey = opt[String](
      "backlog.key",
      descr = Messages("cli.help.backlog.key"),
      required = true,
      noshort = true
    )
    val backlogUrl = opt[String](
      "backlog.url",
      descr = Messages("cli.help.backlog.url"),
      required = true,
      noshort = true
    )
    val redmineKey = opt[String](
      "redmine.key",
      descr = Messages("cli.help.redmine.key"),
      required = true,
      noshort = true
    )
    val redmineUrl = opt[String](
      "redmine.url",
      descr = Messages("cli.help.redmine.url"),
      required = true,
      noshort = true
    )

    val projectKey =
      opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val importOnly =
      opt[Boolean]("importOnly", descr = Messages("cli.help.importOnly"), required = true)
    val exclude =
      opt[List[String]]("exclude", descr = Messages("cli.help.exclude"), required = false)
    val retryCount =
      opt[Int](name = "retryCount", descr = Messages("cli.help.retryCount"), required = false)
    val iaah: ScallopOption[IAAH] =
      opt[String](name = "iaah", descr = "", required = false).map(IAAH(_))
    val help = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  val init = new Subcommand("init") {
    val backlogKey = opt[String](
      "backlog.key",
      descr = Messages("cli.help.backlog.key"),
      required = true,
      noshort = true
    )
    val backlogUrl = opt[String](
      "backlog.url",
      descr = Messages("cli.help.backlog.url"),
      required = true,
      noshort = true
    )
    val redmineKey = opt[String](
      "redmine.key",
      descr = Messages("cli.help.redmine.key"),
      required = true,
      noshort = true
    )
    val redmineUrl = opt[String](
      "redmine.url",
      descr = Messages("cli.help.redmine.url"),
      required = true,
      noshort = true
    )
    val exclude =
      opt[List[String]]("exclude", descr = Messages("cli.help.exclude"), required = false)
    val projectKey =
      opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val help = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  val destroy = new Subcommand("destroy") {
    val backlogKey: ScallopOption[String] = opt[String](
      "backlog.key",
      descr = Messages("cli.help.backlog.key"),
      required = true,
      noshort = true
    )
    val backlogUrl: ScallopOption[String] = opt[String](
      "backlog.url",
      descr = Messages("cli.help.backlog.url"),
      required = true,
      noshort = true
    )
    val projectKey: ScallopOption[String] =
      opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val dryRun: ScallopOption[Boolean] =
      opt[Boolean]("dryRun", descr = Messages("destroy.help.dryRun"), required = false)
    val help: ScallopOption[String] = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  addSubcommand(destroy)
  addSubcommand(execute)
  addSubcommand(init)

  verify()
}

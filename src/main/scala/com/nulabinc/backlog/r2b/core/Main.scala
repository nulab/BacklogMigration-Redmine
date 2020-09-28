package com.nulabinc.backlog.r2b.core

import java.util.Locale

import com.nulabinc.backlog.migration.common.conf.{
  BacklogApiConfiguration,
  BacklogConfiguration,
  ExcludeOption
}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.errors.{MappingFileNotFound, MappingValidationError}
import com.nulabinc.backlog.migration.common.interpreters.{JansiConsoleDSL, LocalStorageDSL}
import com.nulabinc.backlog.migration.common.messages.ConsoleMessages
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.cli.R2BCli
import com.nulabinc.backlog.r2b.conf._
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.nulabinc.backlog.r2b.{AppError, MappingError, OperationCanceled, ValidationError}
import com.osinka.i18n.Messages
import monix.eval.Task
import monix.execution.Scheduler
import org.fusesource.jansi.AnsiConsole
import org.rogach.scallop._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

object R2B extends BacklogConfiguration with Logging {

  private implicit val exc: Scheduler               = monix.execution.Scheduler.Implicits.global
  private implicit val storageDSL: StorageDSL[Task] = LocalStorageDSL()
  private implicit val consoleDSL: ConsoleDSL[Task] = JansiConsoleDSL()

  def main(args: Array[String]): Unit = {
    ConsoleOut.println(s"""|${applicationName}
                 |--------------------------------------------------""".stripMargin)
    AnsiConsole.systemInstall()
    setLang()
    DisableSSLCertificateCheckUtil.disableChecks()
    checkRelease()
    if (ClassVersion.isValid()) {
      try {
        val cli = new CommandLineInterface(args.toIndexedSeq)
        val asyncResult = for {
          result <- execute(cli)
          _ <- result match {
            case Right(_) =>
              Task(Right(()))
            case Left(error: ValidationError) =>
              ConsoleDSL[Task].errorln(MessageResources.validationError(error.errors))
            case Left(error: MappingError) =>
              error.inner match {
                case _: MappingFileNotFound =>
                  ConsoleDSL[Task].errorln(ConsoleMessages.Mappings.needsSetup)
                case e: MappingValidationError[_] =>
                  ConsoleDSL[Task].errorln(ConsoleMessages.Mappings.validationError(e))
                case e =>
                  ConsoleDSL[Task].errorln(e.toString)
              }
            case Left(OperationCanceled) =>
              ConsoleDSL[Task].errorln(MessageResources.cancel)
          }
        } yield ()

        val f = asyncResult.onErrorRecover { ex =>
          logger.error(ex.getMessage, ex)
          exit(1, ex)
        }.runToFuture

        Await.result(f, Duration.Inf)
        AnsiConsole.systemUninstall()
        System.exit(0)
      } catch {
        case e: Throwable =>
          logger.error(e.getMessage, e)
          AnsiConsole.systemUninstall()
          System.exit(1)
      }
    } else {
      ConsoleOut.error(
        Messages("cli.require_java8", System.getProperty("java.specification.version"))
      )
      AnsiConsole.systemUninstall()
      System.exit(1)
    }
  }

  private def exit(exitCode: Int): Unit =
    System.exit(exitCode)

  private def exit(exitCode: Int, error: Throwable): Unit = {
    ConsoleOut.error(
      "ERROR: " + error.getMessage + "\n" + error.printStackTrace()
    )
    exit(exitCode)
  }

  private def execute(cli: CommandLineInterface): Task[Either[AppError, Unit]] =
    cli.subcommand match {
      case Some(cli.execute) if cli.execute.importOnly() =>
        R2BCli.doImport(getConfiguration(cli))
      case Some(cli.execute) =>
        R2BCli.migrate(getConfiguration(cli))
      case Some(cli.init) =>
        R2BCli.init(getConfiguration(cli))
      case _ =>
        R2BCli.help()
    }

  private[this] def getConfiguration(cli: CommandLineInterface) = {
    val keys    = cli.execute.projectKey().split(":")
    val redmine = keys(0)
    val backlog = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

    val retryCount = cli.execute.retryCount.toOption.getOrElse(20)
    val exclude = cli.execute.exclude.toOption.map { args =>
      ExcludeOption(
        issue = args.contains("issue"),
        wiki = args.contains("wiki")
      )
    }.getOrElse(ExcludeOption.default)

    ConsoleOut.println(s"""--------------------------------------------------
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

    AppConfiguration(
      redmineConfig = RedmineApiConfiguration(
        url = cli.execute.redmineUrl(),
        key = cli.execute.redmineKey(),
        projectKey = redmine
      ),
      backlogConfig = BacklogApiConfiguration(
        url = cli.execute.backlogUrl(),
        key = cli.execute.backlogKey(),
        projectKey = backlog
      ),
      exclude = exclude,
      importOnly = cli.execute.importOnly(),
      retryCount = retryCount
    )
  }

  private[this] def setLang(): Unit =
    if (language == "ja") Locale.setDefault(Locale.JAPAN)
    else Locale.setDefault(Locale.US)

  private[this] def checkRelease(): Unit = {
    import java.io._
    import java.net._

    val url          = new URL("https://api.github.com/repos/nulab/BacklogMigration-Redmine/releases")
    val http         = url.openConnection().asInstanceOf[HttpURLConnection]
    val optProxyUser = Option(System.getProperty("https.proxyUser"))
    val optProxyPass = Option(System.getProperty("https.proxyPassword"))

    (optProxyUser, optProxyPass) match {
      case (Some(proxyUser), Some(proxyPass)) =>
        Authenticator.setDefault(new Authenticator() {
          override def getPasswordAuthentication: PasswordAuthentication = {
            new PasswordAuthentication(proxyUser, proxyPass.toCharArray)
          }
        })
      case _ => ()
    }

    try {
      http.setRequestMethod("GET")
      http.connect()

      val reader = new BufferedReader(new InputStreamReader(http.getInputStream))
      val output = new StringBuilder()
      var line   = ""

      while (line != null) {
        line = reader.readLine()
        if (line != null)
          output.append(line)
      }
      reader.close()

      val latest = output.toString().parseJson match {
        case JsArray(releases) if releases.nonEmpty =>
          releases(0).asJsObject.fields.apply("tag_name").convertTo[String].replace("v", "")
        case _ => ""
      }

      if (latest != versionName) {
        ConsoleOut.warning(s"""
             |--------------------------------------------------
             |${Messages("cli.warn.not.latest", latest, versionName)}
             |--------------------------------------------------
        """.stripMargin)
      }
    } catch {
      case ex: Throwable =>
        logger.error(ex.getMessage, ex)
    }
  }

}

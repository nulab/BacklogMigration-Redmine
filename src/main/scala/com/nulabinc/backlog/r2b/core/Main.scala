package com.nulabinc.backlog.r2b.core

import java.util.Locale

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.cli.R2BCli
import com.nulabinc.backlog.r2b.conf._
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.osinka.i18n.Messages
import org.fusesource.jansi.AnsiConsole
import org.rogach.scallop._
import spray.json._
import spray.json.DefaultJsonProtocol._

class CommandLineInterface(arguments: Seq[String]) extends ScallopConf(arguments) with BacklogConfiguration with Logging {

  banner("""Usage: Backlog Migration for Redmine [OPTION]....
      | """.stripMargin)
  footer("\n " + Messages("cli.help"))

  val help    = opt[String]("help", descr = Messages("cli.help.show_help"))
  val version = opt[String]("version", descr = Messages("cli.help.show_version"))

  val execute = new Subcommand("execute") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val importOnly = opt[Boolean]("importOnly", descr = Messages("cli.help.importOnly"), required = true)
    val exclude    = opt[List[String]]("exclude", descr = Messages("cli.help.exclude"), required = false)
    val optOut     = opt[Boolean]("optOut", descr = Messages("cli.help.optOut"), required = false)
    val help       = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  val init = new Subcommand("init") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val help       = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  val destroy = new Subcommand("destroy") {
    val backlogKey: ScallopOption[String] = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl: ScallopOption[String] = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val projectKey: ScallopOption[String] = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val dryRun: ScallopOption[Boolean]    = opt[Boolean]("dryRun", descr = Messages("destroy.help.dryRun"), required = false)
    val help: ScallopOption[String]       = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  addSubcommand(destroy)
  addSubcommand(execute)
  addSubcommand(init)

  verify()
}

object R2B extends BacklogConfiguration with Logging {

  def main(args: Array[String]) {
    ConsoleOut.println(s"""|${applicationName}
                 |--------------------------------------------------""".stripMargin)
    AnsiConsole.systemInstall()
    setLang()
    DisableSSLCertificateCheckUtil.disableChecks()
    checkRelease()
    if (ClassVersion.isValid()) {
      try {
        val cli: CommandLineInterface = new CommandLineInterface(args)
        execute(cli)
        AnsiConsole.systemUninstall()
        System.exit(0)
      } catch {
        case e: Throwable ⇒
          logger.error(e.getMessage, e)
          AnsiConsole.systemUninstall()
          System.exit(1)
      }
    } else {
      ConsoleOut.error(Messages("cli.require_java8", System.getProperty("java.specification.version")))
      AnsiConsole.systemUninstall()
      System.exit(1)
    }
  }

  private[this] def execute(cli: CommandLineInterface) = {
    cli.subcommand match {
      case Some(cli.execute) =>
        if (cli.execute.importOnly()) R2BCli.doImport(getConfiguration(cli))
        else R2BCli.migrate(getConfiguration(cli))
      case Some(cli.init) =>
        R2BCli.init(getConfiguration(cli))
      case Some(cli.destroy) =>
        val config = DestroyConfiguration(
          backlogConfig = BacklogApiConfiguration(
            projectKey = cli.destroy.projectKey().split(":").last.toUpperCase.replaceAll("-", "_"),
            url = cli.destroy.backlogUrl(),
            key = cli.destroy.backlogKey()
          ),
          dryRun = cli.destroy.dryRun.getOrElse(false)
        )
        R2BCli.destroy(config)
      case _ =>
        R2BCli.help()
    }
  }

  private[this] def getConfiguration(cli: CommandLineInterface) = {
    val keys: Array[String] = cli.execute.projectKey().split(":")
    val redmine: String     = keys(0)
    val backlog: String     = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

    ConsoleOut.println(s"""--------------------------------------------------
     |${Messages("common.redmine")} ${Messages("common.url")}[${cli.execute.redmineUrl()}]
     |${Messages("common.redmine")} ${Messages("common.access_key")}[${cli.execute.redmineKey()}]
     |${Messages("common.redmine")} ${Messages("common.project_key")}[${redmine}]
     |${Messages("common.backlog")} ${Messages("common.url")}[${cli.execute.backlogUrl()}]
     |${Messages("common.backlog")} ${Messages("common.access_key")}[${cli.execute.backlogKey()}]
     |${Messages("common.backlog")} ${Messages("common.project_key")}[${backlog}]
     |${Messages("common.importOnly")}[${cli.execute.importOnly()}]
     |${Messages("common.optOut")}[${cli.execute.optOut.toOption.getOrElse(false)}]
     |--------------------------------------------------
     |""".stripMargin)

    AppConfiguration(
      redmineConfig = new RedmineApiConfiguration(url = cli.execute.redmineUrl(), key = cli.execute.redmineKey(), projectKey = redmine),
      backlogConfig = new BacklogApiConfiguration(url = cli.execute.backlogUrl(), key = cli.execute.backlogKey(), projectKey = backlog),
      exclude = cli.execute.exclude.toOption,
      importOnly = cli.execute.importOnly(),
      optOut = cli.execute.optOut())
  }

  private[this] def setLang() = {
    if (language == "ja") {
      Locale.setDefault(Locale.JAPAN)
    } else if (language == "en") {
      Locale.setDefault(Locale.US)
    }
  }

  private[this] def checkRelease() = {
    import java.io._
    import java.net._

    val url = new URL("https://api.github.com/repos/nulab/BacklogMigration-Redmine/releases")
    val http = url.openConnection().asInstanceOf[HttpURLConnection]
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
      var line = ""

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
        ConsoleOut.warning(
          s"""
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

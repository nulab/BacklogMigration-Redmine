package com.nulabinc.r2b.core

import java.util.Locale

import com.nulabinc.backlog.migration.conf.BacklogConfig
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.cli._
import com.nulabinc.r2b.conf._
import com.nulabinc.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.osinka.i18n.{Lang, Messages}
import com.typesafe.config.ConfigFactory
import org.rogach.scallop._

class CommandLineInterface(arguments: Seq[String]) extends ScallopConf(arguments) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  version(ConfigFactory.load().getString("application.title"))

  banner(
    """Usage: Backlog Migration for Redmine [OPTION]....
      | """.stripMargin)
  footer("\n " + Messages("cli.help"))

  val help = opt[String]("help", descr = Messages("cli.help.show_help"))
  val version = opt[String]("version", descr = Messages("cli.help.show_version"))

  val execute = new Subcommand("execute") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val importOnly = opt[Boolean]("importOnly", descr = Messages("cli.help.importOnly"), required = true)
  }

  val init = new Subcommand("init") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
  }

  addSubcommand(execute)
  addSubcommand(init)

  verify()
}

object R2B extends Logging {

  def main(args: Array[String]) {
    DisableSSLCertificateCheckUtil.disableChecks()
    if (ClassVersion.isValid()) {
      try {
        val cli: CommandLineInterface = new CommandLineInterface(args)
        execute(cli)
        System.exit(0)
      } catch {
        case e: Throwable ⇒
          log.error(e.getMessage, e)
          System.exit(1)
      }
    } else {
      log.info(Messages("cli.require_java8", System.getProperty("java.specification.version")))
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
      case _ =>
        R2BCli.help()
    }
  }

  private[this] def getConfiguration(cli: CommandLineInterface) = {
    AppConfiguration(
      redmineConfig = new RedmineConfig(url = cli.execute.redmineUrl(), key = cli.execute.redmineKey()),
      backlogConfig = new BacklogConfig(url = cli.execute.backlogUrl(), key = cli.execute.backlogKey()),
      projectKeyMap = new ProjectKeyMap(cli.execute.projectKey()),
      importOnly = cli.execute.importOnly())
  }

}


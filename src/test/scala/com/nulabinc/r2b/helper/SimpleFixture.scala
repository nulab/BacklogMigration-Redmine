package com.nulabinc.r2b.helper

import java.io.{File, FileInputStream}
import java.util.{Date, Locale, Properties}

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory}
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.backlog.r2b.mapping.core._
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
import com.osinka.i18n.Lang
import com.taskadapter.redmineapi.RedmineManagerFactory
import org.joda.time.DateTime
import spray.json.{JsNumber, JsonParser}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
trait SimpleFixture {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val dateFormat              = "yyyy-MM-dd"
  val timestampFormat: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  val optAppConfiguration: Option[AppConfiguration] = getAppConfiguration

  def redmine() = {
    optAppConfiguration match {
      case Some(appConfiguration) => RedmineManagerFactory.createWithApiKey(appConfiguration.redmineConfig.url, appConfiguration.redmineConfig.key)
      case _                      => throw new RuntimeException()
    }
  }

  def backlog() = {
    optAppConfiguration match {
      case Some(appConfiguration) => getBacklogClient(appConfiguration.backlogConfig)
      case _                      => throw new RuntimeException()
    }
  }

  def redmineProject() = {
    optAppConfiguration match {
      case Some(appConfiguration) => redmine().getProjectManager.getProjectByKey(appConfiguration.redmineConfig.projectKey)
      case _                      => throw new RuntimeException()
    }
  }

  def convertUser(target: String): String = {
    val file = new File(MappingDirectory.USER_MAPPING_FILE)
    if (file.exists()) {
      val userMapping = new ConvertUserMapping()
      userMapping.convert(target)
    } else target
  }

  def convertStatus(target: String): String = {
    val file = new File(MappingDirectory.STATUS_MAPPING_FILE)
    if (file.exists()) {
      val statusMapping = new ConvertStatusMapping()
      statusMapping.convert(target)
    } else target
  }

  def convertPriority(target: String): String = {
    val file = new File(MappingDirectory.PRIORITY_MAPPING_FILE)
    if (file.exists()) {
      val priorityMapping = new ConvertPriorityMapping()
      priorityMapping.convert(target)
    } else target
  }

  private[this] def getAppConfiguration: Option[AppConfiguration] = {
    val file = new File("app.properties")
    if (file.exists()) {
      val prop: Properties = new Properties()
      prop.load(new FileInputStream(file))
      val redmineKey: String = prop.getProperty("redmine.key")
      val redmineUrl: String = prop.getProperty("redmine.url")
      val backlogKey: String = prop.getProperty("backlog.key")
      val backlogUrl: String = prop.getProperty("backlog.url")
      val projectKey: String = prop.getProperty("projectKey")

      val keys: Array[String] = projectKey.split(":")
      val redmine: String     = keys(0)
      val backlog: String     = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

      Some(
        AppConfiguration(redmineConfig = new RedmineApiConfiguration(url = redmineUrl, key = redmineKey, projectKey = redmine),
                         backlogConfig = new BacklogApiConfiguration(url = backlogUrl, key = backlogKey, projectKey = backlog),
                         importOnly = false,
                         optOut = true))
    } else None
  }

  private[this] def getBacklogClient(appConfiguration: BacklogApiConfiguration): BacklogClient = {
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(appConfiguration.url)
    val configure: BacklogConfigure                      = backlogPackageConfigure.apiKey(appConfiguration.key)
    new BacklogClientFactory(configure).newClient()
  }

  def redmineIssueCount(appConfiguration: AppConfiguration) = {
    val string = scala.io.Source
      .fromURL(
        s"${appConfiguration.redmineConfig.url}/issues.json?limit=1&subproject_id=!*&project_id=${redmineProject.getId}&key=${appConfiguration.redmineConfig.key}&status_id=*")
      .mkString
    JsonParser(string).asJsObject.getFields("total_count") match {
      case Seq(JsNumber(totalCount)) => totalCount.intValue()
      case _                         => 0
    }
  }

  def allRedmineIssues(count: Int, offset: Long) = {
    val params = Map("offset" -> offset.toString,
                     "limit"         -> count.toString,
                     "project_id"    -> redmineProject.getId.toString,
                     "status_id"     -> "*",
                     "subproject_id" -> "!*")
    redmine.getIssueManager.getIssues(params.asJava).asScala
  }

  def dateToString(date: Date) = new DateTime(date).toString(dateFormat)

  def timestampToString(date: Date) = new DateTime(date).toString(timestampFormat)

}

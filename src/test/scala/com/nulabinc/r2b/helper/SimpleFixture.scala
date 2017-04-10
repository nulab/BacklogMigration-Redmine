package com.nulabinc.r2b.helper

import java.io.FileInputStream
import java.util.{Date, Locale, Properties}

import com.nulabinc.backlog.migration.conf.BacklogApiConfiguration
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory}
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.controllers.MappingController
import com.nulabinc.r2b.domain.RedmineIssuesWrapper
import com.nulabinc.r2b.domain.RedmineJsonProtocol._
import com.nulabinc.r2b.mapping.core._
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.osinka.i18n.Lang
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import spray.json.JsonParser

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
trait SimpleFixture {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val dateFormat              = "yyyy-MM-dd"
  val timestampFormat: String = "yyyy-MM-dd'T'HH:mm:ssZ"

  val config: AppConfiguration = getConfig
  val redmine: RedmineManager  = RedmineManagerFactory.createWithApiKey(config.redmineConfig.url, config.redmineConfig.key)
  val backlog: BacklogClient   = getBacklogClient(config.backlogConfig)

  val redmineProject = redmine.getProjectManager.getProjectByKey(config.redmineConfig.projectKey)

  val propertyMappingFiles = createMapping(config)
  //TODO
  val userMapping: ConvertUserMapping         = new ConvertUserMapping()
  val statusMapping: ConvertStatusMapping     = new ConvertStatusMapping()
  val priorityMapping: ConvertPriorityMapping = new ConvertPriorityMapping()

  private[this] def getConfig: AppConfiguration = {
    val prop: Properties = new Properties()
    prop.load(new FileInputStream("app.properties"))
    val redmineKey: String = prop.getProperty("redmine.key")
    val redmineUrl: String = prop.getProperty("redmine.url")
    val backlogKey: String = prop.getProperty("backlog.key")
    val backlogUrl: String = prop.getProperty("backlog.url")
    val projectKey: String = prop.getProperty("projectKey")

    val keys: Array[String] = projectKey.split(":")
    val redmine: String     = keys(0)
    val backlog: String     = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

    AppConfiguration(redmineConfig = new RedmineConfig(url = redmineUrl, key = redmineKey, projectKey = redmine),
                     backlogConfig = new BacklogApiConfiguration(url = backlogUrl, key = backlogKey, projectKey = backlog),
                     importOnly = false)
  }

  private[this] def getBacklogClient(config: BacklogApiConfiguration): BacklogClient = {
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(config.url)
    val configure: BacklogConfigure                      = backlogPackageConfigure.apiKey(config.key)
    new BacklogClientFactory(configure).newClient()
  }

  private[this] def createMapping(config: AppConfiguration): PropertyMappingFiles = {
    val mappingData     = MappingController.execute(config.redmineConfig)
    val userMapping     = new UserMappingFile(config.redmineConfig, config.backlogConfig, mappingData)
    val statusMapping   = new StatusMappingFile(config.redmineConfig, config.backlogConfig, mappingData)
    val priorityMapping = new PriorityMappingFile(config.redmineConfig, config.backlogConfig)
    PropertyMappingFiles(user = userMapping, status = statusMapping, priority = priorityMapping)
  }

  def redmineIssueCount() = {
    val countIssueUrl =
      s"${config.redmineConfig.url}/issues.json?limit=1&subproject_id=!*&project_id=${redmineProject.getId}&key=${config.redmineConfig.key}&status_id=*"
    val str: String                                = httpGet(countIssueUrl)
    val redmineIssuesWrapper: RedmineIssuesWrapper = JsonParser(str).convertTo[RedmineIssuesWrapper]
    redmineIssuesWrapper.total_count
  }

  def getRedmineIssues(count: Int, offset: Long) = {
    val params = Map("offset" -> offset.toString,
                     "limit"         -> count.toString,
                     "project_id"    -> redmineProject.getId.toString,
                     "status_id"     -> "*",
                     "subproject_id" -> "!*")
    redmine.getIssueManager.getIssues(params.asJava).asScala
  }

  private[this] def httpGet(url: String): String = {
    val httpGet: HttpGet = new HttpGet(url)
    val client           = new DefaultHttpClient
    val response         = client.execute(httpGet)
    EntityUtils.toString(response.getEntity, "UTF-8")
  }

  def dateToString(date: Date) = new DateTime(date).toString(dateFormat)

  def timestampToString(date: Date) = new DateTime(date).toString(timestampFormat)

}

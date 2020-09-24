package integration.helper

import java.io.{File, FileInputStream}
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.{Date, Locale, Properties}

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, ExcludeOption, MappingDirectory}
import com.nulabinc.backlog.migration.common.dsl.{ConsoleDSL, StorageDSL}
import com.nulabinc.backlog.migration.common.interpreters.{JansiConsoleDSL, LocalStorageDSL}
import com.nulabinc.backlog.migration.common.services.{PriorityMappingFileService, StatusMappingFileService}
import com.nulabinc.backlog.migration.common.utils.IOUtil
import com.nulabinc.backlog.r2b.conf.AppConfiguration
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem}
import com.nulabinc.backlog.r2b.mapping.domain.MappingJsonProtocol._
import com.nulabinc.backlog.r2b.mapping.domain.{Mapping, MappingsWrapper}
import com.nulabinc.backlog.r2b.mapping.service.MappingUserServiceImpl
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory}
import com.osinka.i18n.Lang
import com.taskadapter.redmineapi.bean.Project
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import monix.eval.Task
import monix.execution.Scheduler
import spray.json.{JsNumber, JsonParser}

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
trait SimpleFixture {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  import com.nulabinc.backlog.r2b.deserializers.RedmineMappingDeserializer._

  private implicit val exc: Scheduler               = monix.execution.Scheduler.Implicits.global
  private implicit val storageDSL: StorageDSL[Task] = LocalStorageDSL()
  private implicit val consoleDSL: ConsoleDSL[Task] = JansiConsoleDSL()

  val dateFormat      = new SimpleDateFormat("yyyy-MM-dd")
  val timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

  val optAppConfiguration = getAppConfiguration
  val mappingUserService  = new MappingUserServiceImpl(unmarshal(MappingDirectory.default.userMappingFilePath))

  def redmine: RedmineManager =
    optAppConfiguration match {
      case Some(appConfiguration) => RedmineManagerFactory.createWithApiKey(appConfiguration.redmineConfig.url, appConfiguration.redmineConfig.key)
      case _                      => throw new RuntimeException("App configuration is missing")
    }

  def backlog: BacklogClient =
    optAppConfiguration match {
      case Some(appConfiguration) => getBacklogClient(appConfiguration.backlogConfig)
      case _                      => throw new RuntimeException("App configuration is missing")
    }

  def redmineProject: Project =
    optAppConfiguration match {
      case Some(appConfiguration) => redmine.getProjectManager.getProjectByKey(appConfiguration.redmineConfig.projectKey)
      case _                      => throw new RuntimeException()
    }

  def convertUser(target: String): String = {
    mappingUserService.convert(target)
  }

  def convertStatus(target: String): String =
    StatusMappingFileService
      .getMappings[RedmineStatusMappingItem, Task](
        path = MappingDirectory.default.statusMappingFilePath
      )
      .runSyncUnsafe()
      .getOrElse(throw new RuntimeException("cannot convert status. target: " + target))
      .find(_.src.value == target)
      .getOrElse(throw new RuntimeException("cannot resolve status. target: " + target))
      .optDst
      .map(_.value)
      .getOrElse("")

  def convertPriority(target: String): String =
    PriorityMappingFileService
      .getMappings[RedminePriorityMappingItem, Task](
        path = MappingDirectory.default.priorityMappingFilePath
      )
      .runSyncUnsafe()
      .getOrElse(throw new RuntimeException("cannot convert priority. target: " + target))
      .find(_.src.value == target)
      .getOrElse(throw new RuntimeException("cannot resolve priority. target: " + target))
      .optDst
      .map(_.value)
      .getOrElse("")

  private[this] def unmarshal(path: Path): Seq[Mapping] = {
    val json = IOUtil.input(path).getOrElse("")
    JsonParser(json).convertTo[MappingsWrapper].mappings
  }

  private[this] def getAppConfiguration: Option[AppConfiguration] = {
    val file = new File("app.properties")
    if (file.exists()) {
      val prop: Properties = new Properties()
      prop.load(new FileInputStream(file.getAbsoluteFile))
      val redmineKey: String = prop.getProperty("redmine.key")
      val redmineUrl: String = prop.getProperty("redmine.url")
      val backlogKey: String = prop.getProperty("backlog.key")
      val backlogUrl: String = prop.getProperty("backlog.url")
      val projectKey: String = prop.getProperty("projectKey")

      val keys: Array[String] = projectKey.split(":")
      val redmine: String     = keys(0)
      val backlog: String     = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

      Some(
        AppConfiguration(
          redmineConfig = new RedmineApiConfiguration(url = redmineUrl, key = redmineKey, projectKey = redmine),
          backlogConfig = new BacklogApiConfiguration(url = backlogUrl, key = backlogKey, projectKey = backlog),
          exclude = ExcludeOption.default,
          importOnly = false,
          retryCount = 5
        )
      )
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
        s"${appConfiguration.redmineConfig.url}/issues.json?limit=1&subproject_id=!*&project_id=${redmineProject.getId}&key=${appConfiguration.redmineConfig.key}&status_id=*"
      )
      .mkString
    JsonParser(string).asJsObject.getFields("total_count") match {
      case Seq(JsNumber(totalCount)) => totalCount.intValue
      case _                         => 0
    }
  }

  def allRedmineIssues(count: Int, offset: Long) = {
    val params = Map(
      "offset"        -> offset.toString,
      "limit"         -> count.toString,
      "project_id"    -> redmineProject.getId.toString,
      "status_id"     -> "*",
      "subproject_id" -> "!*"
    )
    redmine.getIssueManager.getIssues(params.asJava).asScala
  }

  def dateToString(date: Date) = dateFormat.format(date)

  def timestampToString(date: Date) = timestampFormat.format(date)

}

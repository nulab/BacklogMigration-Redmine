package com.nulabinc.r2b.redmine.modules

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.domain.{CustomFieldDefinitionSimple, CustomFieldFormats, PropertyValue}
import com.nulabinc.r2b.redmine.service._
import com.taskadapter.redmineapi.bean.Project
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import spray.json.{JsArray, JsBoolean, JsString, JsonParser}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * @author uchida
  */
class RedmineDefaultModule(apiConfig: RedmineConfig) extends AbstractModule {

  override def configure() = {

    //base
    val redmine = createRedmineClient()
    val project = redmine.getProjectManager.getProjectByKey(apiConfig.projectKey)
    bind(classOf[RedmineManager]).toInstance(redmine)
    bind(classOf[Project]).toInstance(project)
    bind(classOf[RedmineConfig]).toInstance(apiConfig)
    bind(classOf[CustomFieldFormats]).toInstance(customFieldFormats())

    val versions      = redmine.getProjectManager.getVersions(project.getId).asScala
    val users         = redmine.getUserManager.getUsers.asScala
    val propertyValue = PropertyValue(versions, users)
    bind(classOf[PropertyValue]).toInstance(propertyValue)
    bind(classOf[Int]).annotatedWith(Names.named("projectId")).toInstance(project.getId)

    //service
    bind(classOf[IssueService]).to(classOf[IssueServiceImpl])
    bind(classOf[MembershipService]).to(classOf[MembershipServiceImpl])
    bind(classOf[ProjectService]).to(classOf[ProjectServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[WikiService]).to(classOf[WikiServiceImpl])
    bind(classOf[CustomFieldService]).to(classOf[CustomFieldServiceImpl])
    bind(classOf[IssueCategoryService]).to(classOf[IssueCategoryServiceImpl])
    bind(classOf[IssuePriorityService]).to(classOf[IssuePriorityServiceImpl])
    bind(classOf[NewsService]).to(classOf[NewsServiceImpl])
    bind(classOf[StatusService]).to(classOf[StatusServiceImpl])
    bind(classOf[TrackerService]).to(classOf[TrackerServiceImpl])
    bind(classOf[VersionService]).to(classOf[VersionServiceImpl])
    bind(classOf[PriorityService]).to(classOf[PriorityServiceImpl])
  }

  private[this] def customFieldFormats(): CustomFieldFormats = {
    val map    = mutable.Map[String, CustomFieldDefinitionSimple]()
    val string = scala.io.Source.fromURL(s"${apiConfig.url}/custom_fields.json?key=${apiConfig.key}").mkString
    JsonParser(string).asJsObject.getFields("custom_fields") match {
      case Seq(JsArray(customFields)) =>
        customFields.foreach { json =>
          json.asJsObject.getFields("name", "field_format", "multiple", "default_value") match {
            case Seq(JsString(name), JsString(field_format), JsBoolean(multiple), JsString(default_value)) =>
              map += name -> CustomFieldDefinitionSimple(field_format, multiple, default_value)
            case _ =>
          }
        }
      case _ =>
    }
    CustomFieldFormats(map)
  }

  private[this] def createRedmineClient(): RedmineManager =
    RedmineManagerFactory.createWithApiKey(apiConfig.url, apiConfig.key)

}

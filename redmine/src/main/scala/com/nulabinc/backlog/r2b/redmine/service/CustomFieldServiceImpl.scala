package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.{Logging, StringUtil}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{RedmineCustomFieldDefinition, RedmineTracker}
import com.taskadapter.redmineapi.bean.{CustomFieldDefinition, Tracker}
import com.taskadapter.redmineapi.{RedmineFormatException, RedmineManager}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsValue, JsonParser}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldServiceImpl @Inject()(apiConfig: RedmineApiConfiguration, redmine: RedmineManager) extends CustomFieldService with Logging {

  override def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition] =
    try {
      redmine.getCustomFieldManager.getCustomFieldDefinitions.asScala.map(toCustomFields)
    } catch {
      case _: RedmineFormatException =>
        val url  = s"${apiConfig.url}/custom_fields.json?key=${apiConfig.key}"
        val json = scala.io.Source.fromURL(url, "UTF-8").mkString
        toCustomFields(json)
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[RedmineCustomFieldDefinition]
    }

  private[this] def toCustomFields(customFieldDefinition: CustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(id = customFieldDefinition.getId,
                                 name = customFieldDefinition.getName,
                                 customizedType = customFieldDefinition.getCustomizedType,
                                 fieldFormat = customFieldDefinition.getFieldFormat,
                                 optRegexp = Option(customFieldDefinition.getRegexp),
                                 optMinLength = Option(customFieldDefinition.getMinLength).map(_.intValue()),
                                 optMaxLength = Option(customFieldDefinition.getMaxLength).map(_.intValue()),
                                 isRequired = customFieldDefinition.isRequired,
                                 isMultiple = customFieldDefinition.isMultiple,
                                 optDefaultValue = StringUtil.notEmpty(customFieldDefinition.getDefaultValue),
                                 trackers = customFieldDefinition.getTrackers.asScala.map(toTracker),
                                 possibleValues = Option(customFieldDefinition.getPossibleValues.asScala).getOrElse(Seq.empty[String]))

  private[this] def toTracker(tracker: Tracker): RedmineTracker =
    RedmineTracker(tracker.getId, tracker.getName)

  private[this] def toCustomFields(json: String): Seq[RedmineCustomFieldDefinition] = {
    JsonParser(json).asJsObject.getFields("custom_fields") match {
      case Seq(JsArray(customFields)) => customFields.map(toCustomField)
      case _                          => Seq.empty[RedmineCustomFieldDefinition]
    }
  }

  private[this] def toCustomField(jsValue: JsValue): RedmineCustomFieldDefinition = {
    RedmineCustomFieldDefinition(id = jsValue.asJsObject.fields.apply("id").convertTo[Int],
                                 name = jsValue.asJsObject.fields.apply("name").convertTo[String],
                                 customizedType = jsValue.asJsObject.fields.apply("customized_type").convertTo[String],
                                 fieldFormat = jsValue.asJsObject.fields.apply("field_format").convertTo[String],
                                 optRegexp = jsValue.asJsObject.fields.get("regexp").map(_.convertTo[String]),
                                 optMinLength = jsValue.asJsObject.fields.get("min_length").map(_.convertTo[Int]),
                                 optMaxLength = jsValue.asJsObject.fields.get("max_length").map(_.convertTo[Int]),
                                 isRequired = jsValue.asJsObject.fields.get("is_required").map(_.convertTo[Boolean]).getOrElse(false),
                                 isMultiple = jsValue.asJsObject.fields.get("multiple").map(_.convertTo[Boolean]).getOrElse(false),
                                 optDefaultValue = jsValue.asJsObject.fields.get("default_value").map(_.convertTo[String]),
                                 trackers = toTrackers(jsValue.asJsObject.fields.get("trackers")),
                                 possibleValues = jsValue.asJsObject.fields.get("possible_values").map(_.convertTo[String]).toSeq)
  }

  private[this] def toTrackers(optJsValue: Option[JsValue]): Seq[RedmineTracker] =
    optJsValue match {
      case Some(jsValue) =>
        jsValue.asJsObject.getFields("tracker") match {
          case Seq(JsArray(trackers)) => trackers.map(toTracker)
          case _                      => Seq.empty[RedmineTracker]
        }
      case _ => Seq.empty[RedmineTracker]
    }

  private[this] def toTracker(jsValue: JsValue): RedmineTracker =
    RedmineTracker(id = jsValue.asJsObject.fields.apply("id").convertTo[Int], name = jsValue.asJsObject.fields.apply("name").convertTo[String])

}

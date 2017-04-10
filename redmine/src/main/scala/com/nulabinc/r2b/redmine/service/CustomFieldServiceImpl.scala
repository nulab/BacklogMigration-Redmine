package com.nulabinc.r2b.redmine.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.domain.RedmineJsonProtocol._
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.taskadapter.redmineapi.bean.{CustomFieldDefinition, Tracker}
import com.taskadapter.redmineapi.{RedmineFormatException, RedmineManager}
import spray.json.JsonParser

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldServiceImpl @Inject()(apiConfig: RedmineConfig, redmine: RedmineManager) extends CustomFieldService with Logging {

  override def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition] =
    try {
      redmine.getCustomFieldManager.getCustomFieldDefinitions.asScala.map(objToCustomFieldDefinition)
    } catch {
      case _: RedmineFormatException =>
        val json    = scala.io.Source.fromURL(s"${apiConfig.url}/custom_fields.json?key=${apiConfig.key}").mkString
        val wrapper = JsonParser(json).convertTo[OldCustomFieldDefinitionsWrapper]
        wrapper.custom_fields.map(oldToCustomFieldDefinition)
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[RedmineCustomFieldDefinition]
    }

  private[this] def objToCustomFieldDefinition(customFieldDefinition: CustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(
      id = customFieldDefinition.getId,
      name = customFieldDefinition.getName,
      customizedType = customFieldDefinition.getCustomizedType,
      fieldFormat = customFieldDefinition.getFieldFormat,
      regexp = Option(customFieldDefinition.getRegexp),
      minLength = Option(customFieldDefinition.getMinLength).map(_.intValue()),
      maxLength = Option(customFieldDefinition.getMaxLength).map(_.intValue()),
      isRequired = customFieldDefinition.isRequired,
      isMultiple = customFieldDefinition.isMultiple,
      defaultValue = if (Option(customFieldDefinition.getDefaultValue).getOrElse("").isEmpty) None else Some(customFieldDefinition.getDefaultValue),
      trackers = customFieldDefinition.getTrackers.asScala.map(getRedmineTracker),
      possibleValues = Option(customFieldDefinition.getPossibleValues.asScala).getOrElse(Seq.empty[String]))

  private[this] def oldToCustomFieldDefinition(oldCustomFieldDefinition: OldCustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(id = oldCustomFieldDefinition.id,
                                 name = oldCustomFieldDefinition.name,
                                 customizedType = oldCustomFieldDefinition.customized_type,
                                 fieldFormat = oldCustomFieldDefinition.field_format,
                                 regexp = oldCustomFieldDefinition.regexp,
                                 minLength = oldCustomFieldDefinition.min_length,
                                 maxLength = oldCustomFieldDefinition.max_length,
                                 isRequired = false,
                                 isMultiple = false,
                                 defaultValue = oldCustomFieldDefinition.default_value,
                                 trackers = redmine.getIssueManager.getTrackers.asScala.map(getRedmineTracker),
                                 possibleValues = oldCustomFieldDefinition.possible_values.getOrElse(Seq.empty[OldPossibleValues]).map(_.value))

  private[this] def getRedmineTracker(tracker: Tracker): RedmineTracker =
    RedmineTracker(tracker.getId, tracker.getName)

}

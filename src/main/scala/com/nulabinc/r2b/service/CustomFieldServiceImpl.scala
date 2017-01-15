package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.domain.RedmineJsonProtocol._
import com.nulabinc.r2b.domain._
import com.taskadapter.redmineapi.bean.{CustomFieldDefinition, Tracker}
import com.taskadapter.redmineapi.{RedmineFormatException, RedmineManager}
import spray.json.JsonParser

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldServiceImpl @Inject()(
                                        @Named("url") url: String,
                                        @Named("key") key: String,
                                        redmine: RedmineManager) extends CustomFieldService with Logging {

  override def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition] =
    try {
      redmine.getCustomFieldManager.getCustomFieldDefinitions.asScala.map(objToCustomFieldDefinition)
    } catch {
      case rfe: RedmineFormatException =>
        val json = scala.io.Source.fromURL(s"${url}/custom_fields.json?key=${key}").mkString
        val wrapper = JsonParser(json).convertTo[OldCustomFieldDefinitionsWrapper]
        wrapper.custom_fields.map(oldToCustomFieldDefinition)
      case e: Throwable =>
        log.error(e)
        Seq.empty[RedmineCustomFieldDefinition]
    }

  private[this] def objToCustomFieldDefinition(cfd: CustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(
      id = cfd.getId,
      name = cfd.getName,
      customizedType = cfd.getCustomizedType,
      fieldFormat = cfd.getFieldFormat,
      regexp = Option(cfd.getRegexp),
      minLength = Option(cfd.getMinLength).map(_.toInt),
      maxLength = Option(cfd.getMaxLength).map(_.toInt),
      isRequired = cfd.isRequired,
      isMultiple = cfd.isMultiple,
      defaultValue = if (Option(cfd.getDefaultValue).getOrElse("").isEmpty) None else Some(cfd.getDefaultValue),
      trackers = cfd.getTrackers.asScala.map(getRedmineTracker),
      possibleValues = Option(cfd.getPossibleValues.asScala).getOrElse(Seq.empty[String]))

  private[this] def oldToCustomFieldDefinition(ocfd: OldCustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(
      id = ocfd.id,
      name = ocfd.name,
      customizedType = ocfd.customized_type,
      fieldFormat = ocfd.field_format,
      regexp = ocfd.regexp,
      minLength = ocfd.min_length,
      maxLength = ocfd.max_length,
      isRequired = false,
      isMultiple = false,
      defaultValue = ocfd.default_value,
      trackers = redmine.getIssueManager.getTrackers.asScala.map(getRedmineTracker),
      possibleValues = ocfd.possible_values.getOrElse(Seq.empty[OldPossibleValues]).map(_.value))

  private[this] def getRedmineTracker(tracker: Tracker): RedmineTracker =
    RedmineTracker(tracker.getId, tracker.getName)

}

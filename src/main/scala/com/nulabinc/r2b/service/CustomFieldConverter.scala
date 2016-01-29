package com.nulabinc.r2b.service

import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain._
import com.taskadapter.redmineapi.RedmineFormatException
import com.taskadapter.redmineapi.bean.{Tracker, CustomFieldDefinition}
import spray.json.JsonParser

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldConverter(conf: R2BConfig) {

  import RedmineJsonProtocol._

  def execute(either: Either[Throwable, Seq[CustomFieldDefinition]]): Seq[RedmineCustomFieldDefinition] = {
    either.fold(
      e => e match {
        case rfe: RedmineFormatException =>
          val url = conf.redmineUrl + "/custom_fields.json?key=" + conf.redmineKey
          val json = scala.io.Source.fromURL(url).mkString
          val wrapper = JsonParser(json).convertTo[OldCustomFieldDefinitionsWrapper]
          wrapper.custom_fields.map(oldToCustomFieldDefinition)
      },
      cfds => cfds.map(objToCustomFieldDefinition)
    )
  }

  private def objToCustomFieldDefinition(cfd: CustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(
      id = cfd.getId,
      name = cfd.getName,
      customizedType = cfd.getCustomizedType,
      fieldFormat = cfd.getFieldFormat,
      regexp = Option(cfd.getRegexp),
      minLength = Option(cfd.getMinLength).map(_.toInt),
      maxLength = Option(cfd.getMaxLength).map(_.toInt),
      isRequired = cfd.isRequired,
      isFilter = cfd.isFilter,
      isSearchable = cfd.isSearchable,
      isMultiple = cfd.isMultiple,
      isVisible = cfd.isVisible,
      defaultValue = if (cfd.getDefaultValue == null || cfd.getDefaultValue.isEmpty) None else Some(cfd.getDefaultValue),
      trackers = cfd.getTrackers.asScala.map(getRedmineTracker),
      possibleValues = if (cfd.getPossibleValues == null) Seq.empty[String] else cfd.getPossibleValues.asScala)

  private def getRedmineTracker(tracker: Tracker): RedmineTracker =
    RedmineTracker(tracker.getId, tracker.getName)

  private def oldToCustomFieldDefinition(ocfd: OldCustomFieldDefinition): RedmineCustomFieldDefinition =
    RedmineCustomFieldDefinition(
      id = ocfd.id,
      name = ocfd.name,
      customizedType = ocfd.customized_type,
      fieldFormat = ocfd.field_format,
      regexp = ocfd.regexp,
      minLength = None,
      maxLength = None,
      isRequired = false,
      isFilter = false,
      isSearchable = false,
      isMultiple = false,
      isVisible = ocfd.visible,
      defaultValue = ocfd.default_value,
      trackers = Seq(ocfd.trackers.tracker),
      possibleValues = Seq.empty[String])

}

package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.domain.{PropertyValue, RedmineCustomFieldDefinition}
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class CustomFieldValueWrites @Inject()(propertyValue: PropertyValue) extends Writes[(String, Option[String]), Option[String]] with Logging {

  override def writes(customFieldValue: (String, Option[String])): Option[String] = {
    val (strId, optValue)        = customFieldValue
    val optCustomFieldDefinition = propertyValue.customFieldDefinitionOfId(strId)
    optCustomFieldDefinition match {
      case Some(customFieldDefinition) => convert(customFieldDefinition, optValue)
      case _                           => optValue
    }
  }

  private[this] def convert(customFieldDefinition: RedmineCustomFieldDefinition, optValue: Option[String]) =
    customFieldDefinition.fieldFormat match {
      case RedmineConstantValue.FieldFormat.VERSION =>
        propertyValue.versionOfId(optValue).map(_.getName)
      case RedmineConstantValue.FieldFormat.USER =>
        propertyValue.optUserOfId(optValue).map(_.getFullName)
      case RedmineConstantValue.FieldFormat.BOOL =>
        optValue match {
          case Some("0") => Some(Messages("common.no"))
          case Some("1") => Some(Messages("common.yes"))
          case _         => None
        }
      case _ => optValue
    }

}

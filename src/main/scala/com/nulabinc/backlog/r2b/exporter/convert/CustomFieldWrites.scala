package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogCustomField
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue
import com.nulabinc.backlog4j.CustomField.FieldType
import com.taskadapter.redmineapi.bean.CustomField

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
private[exporter] class CustomFieldWrites @Inject()(propertyValue: PropertyValue, customFieldValueWrites: CustomFieldValueWrites)
    extends Writes[CustomField, Option[BacklogCustomField]]
    with Logging {

  override def writes(customField: CustomField): Option[BacklogCustomField] = {
    val optCustomFieldDefinition = propertyValue.customFieldDefinitionOfName(customField.getName)
    optCustomFieldDefinition match {
      case Some(customFieldDefinition) =>
        customFieldDefinition.fieldFormat match {
          case RedmineConstantValue.FieldFormat.TEXT                                           => Some(toTextCustomField(customField))
          case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK => Some(toTextAreaCustomField(customField))
          case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT   => Some(toNumericCustomField(customField))
          case RedmineConstantValue.FieldFormat.DATE                                           => Some(toDateCustomField(customField))
          case RedmineConstantValue.FieldFormat.BOOL                                           => Some(bool(customField))
          case RedmineConstantValue.FieldFormat.LIST if (!customFieldDefinition.isMultiple)    => Some(toSingleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.LIST if (customFieldDefinition.isMultiple)     => Some(toMultipleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.ENUMERATION if (!customFieldDefinition.isMultiple)    => Some(toSingleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.ENUMERATION if (customFieldDefinition.isMultiple)     => Some(toMultipleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.VERSION                                        => Some(version(customField))
          case RedmineConstantValue.FieldFormat.USER                                           => Some(user(customField))
          case _                                                                               => None
        }
      case _ => None
    }
  }

  private[this] def toTextCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.Text.getIntValue,
      optValue = Option(customField.getValue),
      values = Seq.empty[String]
    )
  }

  private[this] def toTextAreaCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.TextArea.getIntValue,
      optValue = Option(customField.getValue),
      values = Seq.empty[String]
    )
  }

  private[this] def toNumericCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.Numeric.getIntValue,
      optValue = Option(customField.getValue),
      values = Seq.empty[String]
    )
  }

  private[this] def toDateCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.Date.getIntValue,
      optValue = Option(customField.getValue),
      values = Seq.empty[String]
    )
  }

  private[this] def toSingleListCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Option(customField.getValue),
      values = Seq.empty[String]
    )
  }

  private[this] def toMultipleListCustomField(customField: CustomField): BacklogCustomField = {
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.MultipleList.getIntValue,
      optValue = None,
      values = customField.getValues.asScala
    )
  }

  private[this] def version(customField: CustomField): BacklogCustomField =
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Convert.toBacklog((customField.getId.toString, Option(customField.getValue)))(customFieldValueWrites),
      values = Seq.empty[String]
    )

  private[this] def user(customField: CustomField): BacklogCustomField =
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Convert.toBacklog((customField.getId.toString, Option(customField.getValue)))(customFieldValueWrites),
      values = Seq.empty[String]
    )

  private[this] def bool(customField: CustomField): BacklogCustomField =
    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Convert.toBacklog((customField.getId.toString, Option(customField.getValue)))(customFieldValueWrites),
      values = Seq.empty[String]
    )

}

package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogCustomField
import com.nulabinc.backlog.migration.utils.StringUtil
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.r2b.redmine.domain.PropertyValue
import com.taskadapter.redmineapi.bean.{CustomField, User, Version}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class CustomFieldWrites @Inject()(propertyValue: PropertyValue) extends Writes[CustomField, Option[BacklogCustomField]] {

  override def writes(customField: CustomField): Option[BacklogCustomField] = {
    val optCustomFieldDefinition = propertyValue.customFieldDefinitionOfName(customField.getName)
    optCustomFieldDefinition match {
      case Some(customFieldDefinition) =>
        customFieldDefinition.fieldFormat match {
          case RedmineConstantValue.FieldFormat.TEXT                                           => Some(toTextCustomField(customField))
          case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK => Some(toTextAreaCustomField(customField))
          case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT   => Some(toNumericCustomField(customField))
          case RedmineConstantValue.FieldFormat.DATE                                           => Some(toDateCustomField(customField))
          case RedmineConstantValue.FieldFormat.BOOL                                           => Some(toSingleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.LIST if (!customFieldDefinition.isMultiple)    => Some(toSingleListCustomField(customField))
          case RedmineConstantValue.FieldFormat.LIST if (customFieldDefinition.isMultiple)     => Some(toMultipleListCustomField(customField))
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

  private[this] def version(customField: CustomField): BacklogCustomField = {
    def condition(version: Version, value: String) = {
      StringUtil.safeStringToInt(value) match {
        case Some(intValue) => intValue == version.getId.intValue()
        case _              => false
      }
    }

    def toName(value: String) = {
      propertyValue.versions.find(version => condition(version, value))
    }

    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Option(customField.getValue).flatMap(toName).map(_.getName),
      values = Seq.empty[String]
    )
  }

  private[this] def user(customField: CustomField): BacklogCustomField = {
    def condition(user: User, value: String) = {
      StringUtil.safeStringToInt(value) match {
        case Some(intValue) => intValue == user.getId.intValue()
        case _              => false
      }
    }

    def toName(value: String): Option[User] = {
      propertyValue.users.find(user => condition(user, value))
    }

    BacklogCustomField(
      name = customField.getName,
      fieldTypeId = FieldType.SingleList.getIntValue,
      optValue = Option(customField.getValue).flatMap(toName).map(_.getFullName),
      values = Seq.empty[String]
    )
  }

}

package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineConstantValue
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineCustomFieldDefinition}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.internal.json.customFields.DateCustomFieldSetting
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
private[exporter] class CustomFieldDefinitionsWrites @Inject()(propertyValue: PropertyValue)
    extends Writes[Seq[RedmineCustomFieldDefinition], Seq[BacklogCustomFieldSetting]]
    with Logging {

  override def writes(redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): Seq[BacklogCustomFieldSetting] = {
    redmineCustomFieldDefinitions.map(toBacklog)
  }

  private[this] def toBacklog(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldSetting =
    BacklogCustomFieldSetting(optId = Some(redmineCustomFieldDefinition.id.toLong),
                              name = redmineCustomFieldDefinition.name,
                              description = "",
                              typeId = typeId(redmineCustomFieldDefinition),
                              required = redmineCustomFieldDefinition.isRequired,
                              applicableIssueTypes = redmineCustomFieldDefinition.trackers.map(_.name),
                              delete = false,
                              property = property(redmineCustomFieldDefinition))

  private[this] def property(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldProperty =
    redmineCustomFieldDefinition.fieldFormat match {
      case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK | RedmineConstantValue.FieldFormat.TEXT => textProperty()
      case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT                                           => numericProperty(redmineCustomFieldDefinition)
      case RedmineConstantValue.FieldFormat.DATE                                                                                   => dateProperty(redmineCustomFieldDefinition)
      case RedmineConstantValue.FieldFormat.LIST | RedmineConstantValue.FieldFormat.USER | RedmineConstantValue.FieldFormat.VERSION |
          RedmineConstantValue.FieldFormat.BOOL | RedmineConstantValue.FieldFormat.ENUMERATION =>
        multipleProperty(redmineCustomFieldDefinition)
    }

  private[this] def multipleProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldMultipleProperty =
    BacklogCustomFieldMultipleProperty(typeId = multipleTypeId(redmineCustomFieldDefinition),
                                       items = possibleValues(redmineCustomFieldDefinition).map(toBacklogItem),
                                       allowAddItem = true,
                                       allowInput = false)

  private[this] def dateProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDateProperty =
    BacklogCustomFieldDateProperty(typeId = BacklogConstantValue.CustomField.Date,
                                   optInitialDate = optInitialValueDate(redmineCustomFieldDefinition),
                                   optMin = None,
                                   optMax = None)

  private[this] def numericProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldNumericProperty =
    BacklogCustomFieldNumericProperty(typeId = BacklogConstantValue.CustomField.Numeric,
                                      optInitialValue = initialValueNumeric(redmineCustomFieldDefinition),
                                      optUnit = None,
                                      optMin = minNumeric(redmineCustomFieldDefinition.optMinLength),
                                      optMax = maxNumeric(redmineCustomFieldDefinition.optMaxLength))

  private[this] def textProperty(): BacklogCustomFieldTextProperty =
    BacklogCustomFieldTextProperty(BacklogConstantValue.CustomField.Text)

  private[this] def possibleValues(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Seq[String] =
    redmineCustomFieldDefinition.fieldFormat match {
      case RedmineConstantValue.FieldFormat.VERSION => propertyValue.versions.map(_.getName)
      case RedmineConstantValue.FieldFormat.USER    => propertyValue.memberships.filter(_.getUser != null).map(_.getUser.getFullName)
      case RedmineConstantValue.FieldFormat.BOOL    => booleanPossibleValues()
      case _                                        => redmineCustomFieldDefinition.possibleValues
    }

  private[this] def initialValueNumeric(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[Float] =
    (redmineCustomFieldDefinition.fieldFormat, redmineCustomFieldDefinition.optDefaultValue) match {
      case ("int" | "float", Some(defaultValue)) if (defaultValue.nonEmpty) => Some(defaultValue.toFloat)
      case _                                                                => None
    }

  private[this] def minNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, ((value - 1) * (-1)).toDouble).toFloat)

  private[this] def maxNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, value.toDouble).toFloat)

  private[this] def optInitialValueDate(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[BacklogCustomFieldInitialDate] =
    if (redmineCustomFieldDefinition.fieldFormat == RedmineConstantValue.FieldFormat.DATE) {
      val initialDate =
        BacklogCustomFieldInitialDate(typeId = DateCustomFieldSetting.InitialValueType.FixedDate.getIntValue.toLong,
                                      optDate = redmineCustomFieldDefinition.optDefaultValue,
                                      optShift = None)
      Some(initialDate)
    } else None

  private[this] def booleanPossibleValues(): Seq[String] = Seq(Messages("common.no"), Messages("common.yes"))

  private[this] def toBacklogItem(name: String): BacklogItem =
    BacklogItem(optId = None, name = name)

  private[this] def typeId(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Int =
    redmineCustomFieldDefinition.fieldFormat match {
      case RedmineConstantValue.FieldFormat.STRING | RedmineConstantValue.FieldFormat.LINK => FieldType.Text.getIntValue
      case RedmineConstantValue.FieldFormat.INT | RedmineConstantValue.FieldFormat.FLOAT   => FieldType.Numeric.getIntValue
      case RedmineConstantValue.FieldFormat.DATE                                           => FieldType.Date.getIntValue
      case RedmineConstantValue.FieldFormat.TEXT                                           => FieldType.TextArea.getIntValue
      case RedmineConstantValue.FieldFormat.LIST =>
        if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
      case RedmineConstantValue.FieldFormat.USER | RedmineConstantValue.FieldFormat.VERSION =>
        if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
      case RedmineConstantValue.FieldFormat.BOOL => FieldType.Radio.getIntValue
      case RedmineConstantValue.FieldFormat.ENUMERATION => FieldType.SingleList.getIntValue
    }

  private[this] def multipleTypeId(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Int = {
    if (redmineCustomFieldDefinition.isMultiple) BacklogConstantValue.CustomField.MultipleList
    else BacklogConstantValue.CustomField.SingleList
  }

}

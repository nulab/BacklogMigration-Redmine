package com.nulabinc.r2b.service.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogProperty
import com.nulabinc.backlog.migration.domain._
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.internal.json.customFields.DateCustomFieldSetting
import com.nulabinc.r2b.conf.RedmineProperty
import com.nulabinc.r2b.domain.RedmineCustomFieldDefinition
import com.nulabinc.r2b.service.PropertyService
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class ConvertCustomFieldDefinitionServiceImpl @Inject()(propertyService: PropertyService) extends ConvertCustomFieldDefinitionService with Logging {

  override def convert(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDefinition =
    BacklogCustomFieldDefinition(
      id = redmineCustomFieldDefinition.id.toLong,
      name = redmineCustomFieldDefinition.name,
      description = "",
      typeId = getTypeId(redmineCustomFieldDefinition),
      required = redmineCustomFieldDefinition.isRequired,
      applicableIssueTypes = redmineCustomFieldDefinition.trackers.map(_.name),
      property = getProperty(redmineCustomFieldDefinition))

  private[this] def getProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldProperty =
    redmineCustomFieldDefinition.fieldFormat match {
      case RedmineProperty.FieldFormat.STRING | RedmineProperty.FieldFormat.LINK | RedmineProperty.FieldFormat.TEXT => getTextProperty()
      case RedmineProperty.FieldFormat.INT | RedmineProperty.FieldFormat.FLOAT => getNumericProperty(redmineCustomFieldDefinition)
      case RedmineProperty.FieldFormat.DATE => getDateProperty(redmineCustomFieldDefinition)
      case RedmineProperty.FieldFormat.LIST | RedmineProperty.FieldFormat.USER | RedmineProperty.FieldFormat.VERSION | RedmineProperty.FieldFormat.BOOL => getMultipleProperty(redmineCustomFieldDefinition)
    }

  private[this] def getMultipleProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldMultipleProperty =
    BacklogCustomFieldMultipleProperty(
      typeId = if (redmineCustomFieldDefinition.isMultiple) BacklogProperty.CustomField.MultipleList else BacklogProperty.CustomField.SingleList,
      items = getPossibleValues(redmineCustomFieldDefinition),
      allowAddItem = true,
      allowInput = false)

  private[this] def getDateProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDateProperty =
    BacklogCustomFieldDateProperty(
      typeId = BacklogProperty.CustomField.Date,
      initialDate = getInitialValueDate(redmineCustomFieldDefinition),
      min = None,
      max = None)

  private[this] def getNumericProperty(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldNumericProperty =
    BacklogCustomFieldNumericProperty(
      typeId = BacklogProperty.CustomField.Numeric,
      initialValue = getInitialValueNumeric(redmineCustomFieldDefinition),
      unit = None,
      min = getMinNumeric(redmineCustomFieldDefinition.maxLength),
      max = getMaxNumeric(redmineCustomFieldDefinition.maxLength))

  private[this] def getTextProperty(): BacklogCustomFieldTextProperty =
    BacklogCustomFieldTextProperty(BacklogProperty.CustomField.Text)

  private[this] def getPossibleValues(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Seq[String] =
    redmineCustomFieldDefinition.fieldFormat match {
      case RedmineProperty.FieldFormat.VERSION => propertyService.allVersionNames()
      case RedmineProperty.FieldFormat.USER => propertyService.allMembershipNames()
      case RedmineProperty.FieldFormat.BOOL => booleanPossibleValues()
      case _ => redmineCustomFieldDefinition.possibleValues
    }

  private[this] def getInitialValueNumeric(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[Float] =
    (redmineCustomFieldDefinition.fieldFormat, redmineCustomFieldDefinition.defaultValue) match {
      case ("int" | "float", Some(defaultValue)) if (defaultValue.nonEmpty) => Some(defaultValue.toFloat)
      case _ => None
    }

  private[this] def getMinNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, ((value - 1) * (-1)).toDouble).toFloat)

  private[this] def getMaxNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, value.toDouble).toFloat)

  private[this] def getInitialValueDate(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[BacklogCustomFieldInitialDate] =
    if (redmineCustomFieldDefinition.fieldFormat == RedmineProperty.FieldFormat.DATE)
      Some(BacklogCustomFieldInitialDate(
        typeId = DateCustomFieldSetting.InitialValueType.FixedDate.getIntValue.toLong,
        date = redmineCustomFieldDefinition.defaultValue,
        shift = None))
    else None

  private[this] def booleanPossibleValues(): Seq[String] = Seq(Messages("common.no"), Messages("common.yes"))

  private[this] def getTypeId(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Int = redmineCustomFieldDefinition.fieldFormat match {
    case RedmineProperty.FieldFormat.STRING | RedmineProperty.FieldFormat.LINK => FieldType.Text.getIntValue
    case RedmineProperty.FieldFormat.INT | RedmineProperty.FieldFormat.FLOAT => FieldType.Numeric.getIntValue
    case RedmineProperty.FieldFormat.DATE => FieldType.Date.getIntValue
    case RedmineProperty.FieldFormat.TEXT => FieldType.TextArea.getIntValue
    case RedmineProperty.FieldFormat.LIST => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
    case RedmineProperty.FieldFormat.USER | RedmineProperty.FieldFormat.VERSION => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
    case RedmineProperty.FieldFormat.BOOL => FieldType.Radio.getIntValue
  }
}
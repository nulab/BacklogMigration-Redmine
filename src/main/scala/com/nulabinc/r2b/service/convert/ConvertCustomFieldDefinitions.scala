package com.nulabinc.r2b.service.convert

import java.util.Locale

import com.nulabinc.backlog.importer.domain.{BacklogCustomFieldDefinition, BacklogCustomFieldDefinitionsWrapper}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.r2b.actor.convert.utils.ProjectContext
import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.RedmineCustomFieldDefinition
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class ConvertCustomFieldDefinitions(pctx: ProjectContext) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def execute(redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition]): BacklogCustomFieldDefinitionsWrapper =
    BacklogCustomFieldDefinitionsWrapper(redmineCustomFieldDefinitions.map(redmineCustomFieldDefinition => getBacklogCustomFieldDefinition(redmineCustomFieldDefinition)))

  private def getBacklogCustomFieldDefinition(customFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDefinition =
    BacklogCustomFieldDefinition(
      id = customFieldDefinition.id,
      name = customFieldDefinition.name,
      description = "",
      typeId = getTypeId(customFieldDefinition),
      required = customFieldDefinition.isRequired,
      applicableIssueTypes = customFieldDefinition.trackers.map(_.name),
      items = getPossibleValues(customFieldDefinition),
      initialValueNumeric = getInitialValueNumeric(customFieldDefinition),
      minNumeric = getMinNumeric(customFieldDefinition.maxLength),
      maxNumeric = getMaxNumeric(customFieldDefinition.maxLength),
      initialValueDate = getInitialValueDate(customFieldDefinition),
      minDate = None,
      maxDate = None)

  private def getPossibleValues(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Seq[String] =
    redmineCustomFieldDefinition.fieldFormat match {
      case ConfigBase.FieldFormat.VERSION => pctx.getVersions()
      case ConfigBase.FieldFormat.USER => pctx.getMemberships()
      case ConfigBase.FieldFormat.BOOL => booleanPossibleValues()
      case _ => redmineCustomFieldDefinition.possibleValues
    }

  private def getInitialValueNumeric(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[Float] =
    if (redmineCustomFieldDefinition.fieldFormat == "int" || redmineCustomFieldDefinition.fieldFormat == "float")
      redmineCustomFieldDefinition.defaultValue.map(_.toFloat)
    else None

  private def getMinNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, (value - 1) * (-1)).toFloat)

  private def getMaxNumeric(value: Option[Int]): Option[Float] =
    value.map(value => math.pow(10, value).toFloat)

  private def getInitialValueDate(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Option[String] =
    if (redmineCustomFieldDefinition.fieldFormat == ConfigBase.FieldFormat.DATE) redmineCustomFieldDefinition.defaultValue
    else None

  private def booleanPossibleValues(): Seq[String] = Seq(Messages("label.no"), Messages("label.yes"))

  private def getTypeId(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): Int = redmineCustomFieldDefinition.fieldFormat match {
    case ConfigBase.FieldFormat.STRING | ConfigBase.FieldFormat.LINK => FieldType.Text.getIntValue
    case ConfigBase.FieldFormat.INT | ConfigBase.FieldFormat.FLOAT => FieldType.Numeric.getIntValue
    case ConfigBase.FieldFormat.DATE => FieldType.Date.getIntValue
    case ConfigBase.FieldFormat.TEXT => FieldType.TextArea.getIntValue
    case ConfigBase.FieldFormat.LIST => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
    case ConfigBase.FieldFormat.USER | ConfigBase.FieldFormat.VERSION => if (redmineCustomFieldDefinition.isMultiple) FieldType.MultipleList.getIntValue else FieldType.SingleList.getIntValue
    case ConfigBase.FieldFormat.BOOL => FieldType.Radio.getIntValue
  }
}
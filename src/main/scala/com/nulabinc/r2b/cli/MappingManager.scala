package com.nulabinc.r2b.cli

import java.util.Locale

import com.nulabinc.r2b.actor.utils.Logging
import com.nulabinc.r2b.domain.MappingJsonProtocol._
import com.nulabinc.r2b.domain.{Mapping, MappingItem, MappingsWrapper}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.{Lang, Messages}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{JsonParser, _}

import scalax.file.Path

/**
 * @author uchida
 */
trait MappingManager extends Logging {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val OTHER_MAPPING: Boolean = true
  val COMMAND_FINISH: Boolean = false

  def matchWithBacklog(redmine: MappingItem): String

  def backlogs: Seq[MappingItem]

  def redmines: Seq[MappingItem]

  def filePath: String

  def itemName: String

  def description: String

  def isDisplayDetail: Boolean

  def isValid: Boolean = getErrors.isEmpty

  def isExists: Boolean = {
    val path: Path = Path.fromString(filePath)
    path.isFile
  }

  def isParsed: Boolean = unmarshal().isRight

  def createExec() = {
    if (isExists) {
      if (confirmRecreate()) doCreate()
    } else doCreate()
  }

  private def doCreate() = {
    createFile()
    printlog(Messages("mapping.output_mapping_file", itemName, filePath))
  }

  def show() = {
    printlog(Messages("mapping.show", itemName))
    printlog("--------------------------------------------------")

    val either: Either[Throwable, Seq[Mapping]] = unmarshal().right.map(_.mappings)
    val mappings: Seq[Mapping] = either.right.get
    mappings.foreach(showMapping)

    printlog("--------------------------------------------------")
    printlog()
  }

  def showBrokenFileMessage() =
    if (!isParsed) {
      printlog()
      printlog(Messages("mapping.broken_file", itemName))
      printlog("--------------------------------------------------")
      printlog(Messages("mapping.need_fix_file", filePath))
      printlog("--------------------------------------------------")
    }

  def showInvalidErrors() =
    if (!isValid) {
      printlog()
      printlog(Messages("mapping.show_error", itemName))
      printlog("--------------------------------------------------")
      val errors: Seq[String] = getErrors
      errors.foreach(printlog(_))
      printlog()
      printlog(Messages("mapping.need_fix_file", filePath))
      printlog("--------------------------------------------------")
    }

  private def unmarshal(): Either[Throwable, MappingsWrapper] = {
    val path: Path = Path.fromString(filePath)
    val json = path.lines().mkString
    try {
      val wrapper: MappingsWrapper = JsonParser(json).convertTo[MappingsWrapper]
      Right(wrapper)
    } catch {
      case e: Throwable => Left(e)
    }
  }

  private def getErrors: Seq[String] = {
    val validator: Validator = new Validator()
    validator.validate()
  }

  private def createFile() = {
    val mappings: Seq[Mapping] = redmines.map(convertMapping)
    IOUtil.output(filePath, MappingsWrapper(description, mappings).toJson.prettyPrint)
  }

  private def confirmRecreate(): Boolean = {
    printlog()
    val input: String = scala.io.StdIn.readLine(Messages("mapping.confirm_recreate", itemName, filePath))
    input == "y" || input == "Y"
  }

  private def showMapping(mapping: Mapping) =
    printlog("- " + getDisplay(mapping.redmine, redmines) + " => " + getDisplay(mapping.backlog, backlogs))

  private def getDisplay(name: String, mappingItems: Seq[MappingItem]): String = {
    val option: Option[MappingItem] = mappingItems.find(_.name == name)
    if (option.isDefined) {
      val mappingItem: MappingItem = option.get
      if (isDisplayDetail) mappingItem.display + "(" + mappingItem.name + ")"
      else name
    } else name
  }

  private def convertMapping(redmine: MappingItem): Mapping = Mapping(matchWithBacklog(redmine), redmine.name)

  private class Validator {
    def validate(): Seq[String] = {
      val either: Either[Throwable, MappingsWrapper] = unmarshal()
      val mappings: Seq[Mapping] = either.right.get.mappings

      val errors: Seq[String] = validateItemsExists(mappings)
      errors union validateItemsRequired(mappings) union validateRedmineItemsExists(mappings)
    }

    private def validateRedmineItemsExists(mappings: Seq[Mapping]): Seq[String] =
      redmines.foldLeft(Seq.empty[String])((errors: Seq[String], mappingItem: MappingItem) => {
        val error: Option[String] = validateRedmineItemExists(mappingItem, mappings)
        error match {
          case Some(error) => errors :+ error
          case None => errors
        }
      })

    private def validateRedmineItemExists(mappingItem: MappingItem, mappings: Seq[Mapping]): Option[String] =
      if (!mappings.exists(mapping => mapping.redmine == mappingItem.name))
        Some("- " + Messages("mapping.not_exists_redmine_item", itemName, mappingItem.name, filePath))
      else None


    private def validateItemsExists(mappings: Seq[Mapping]): Seq[String] =
      mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
        val error: Option[String] = validateItemExists(mapping)
        error match {
          case Some(error) => errors :+ error
          case None => errors
        }
      })

    private def validateItemExists(mapping: Mapping): Option[String] =
      if (mapping.backlog.nonEmpty && !backlogs.exists(_.name == mapping.backlog)) {
        Some("- " + Messages("mapping.not_exist_backlog", mapping.backlog, itemName))
      } else if (mapping.redmine.nonEmpty && !redmines.exists(_.name == mapping.redmine)) {
        Some("- " + Messages("mapping.not_exist_redmine", mapping.redmine, itemName))
      } else None

    private def validateItemsRequired(mappings: Seq[Mapping]): Seq[String] =
      mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
        val error: Option[String] = validateItemRequired(mapping)
        error match {
          case Some(error) => errors :+ error
          case None => errors
        }
      })

    private def validateItemRequired(mapping: Mapping): Option[String] =
      if (mapping.backlog.isEmpty) Some("- " + Messages("mapping.specify_item", itemName, mapping.redmine))
      else None
  }

}
package com.nulabinc.r2b.mapping

import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.cli.MappingValidator
import com.nulabinc.r2b.mapping.MappingJsonProtocol._
import spray.json.{JsonParser, _}

import scalax.file.Path

/**
  * @author uchida
  */
trait MappingFile extends Logging {

  val OTHER_MAPPING: Boolean = true
  val COMMAND_FINISH: Boolean = false

  def matchWithBacklog(redmine: MappingItem): String

  def backlogs: Seq[MappingItem]

  def redmines: Seq[MappingItem]

  def filePath: String

  def itemName: String

  def description: String

  def isDisplayDetail: Boolean

  def isValid: Boolean = errors.isEmpty

  def isExists: Boolean = {
    val path: Path = Path.fromString(filePath)
    path.isFile
  }

  def isParsed: Boolean = unmarshal().isRight

  def create() =
    IOUtil.output(filePath, MappingsWrapper(description, redmines.map(convert)).toJson.prettyPrint)

  def unmarshal(): Either[Throwable, MappingsWrapper] = {
    val path: Path = Path.fromString(filePath)
    val json = path.lines().mkString
    try {
      val wrapper: MappingsWrapper = JsonParser(json).convertTo[MappingsWrapper]
      Right(wrapper)
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
        Left(e)
    }
  }

  def errors: Seq[String] = {
    val validator = new MappingValidator(redmines, backlogs, itemName, filePath)
    validator.validate(unmarshal())
  }

  def display(name: String, mappingItems: Seq[MappingItem]): String = {
    val option: Option[MappingItem] = mappingItems.find(_.name == name)
    if (option.isDefined) {
      val mappingItem: MappingItem = option.get
      if (isDisplayDetail) s"${mappingItem.display}(${mappingItem.name})"
      else name
    } else name
  }

  private[this] def convert(redmine: MappingItem): Mapping = Mapping(redmine.name, matchWithBacklog(redmine))

}
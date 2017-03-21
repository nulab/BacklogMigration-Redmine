package com.nulabinc.r2b.service

import com.nulabinc.r2b.mapping.MappingJsonProtocol._
import com.nulabinc.r2b.mapping.{Mapping, MappingDirectory, MappingsWrapper}
import spray.json.JsonParser

import scalax.file.Path

/**
  * @author uchida
  */
trait ConvertMapping {

  def filePath: String

  def convert(target: String): String =
    if (mappings.isEmpty) target
    else
      mappings.find(_.redmine == target) match {
        case Some(mapping) =>
          if (mapping.backlog.nonEmpty) mapping.backlog else target
        case _ => target
      }

  private[this] val mappings: Seq[Mapping] = unmarshal(filePath)

  private[this] def unmarshal(strPath: String): Seq[Mapping] = {
    val path: Path = Path.fromString(strPath)
    val json = path.lines().mkString
    JsonParser(json).convertTo[MappingsWrapper].mappings
  }

}

class ConvertUserMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.USER_MAPPING_FILE

}

class ConvertStatusMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.STATUS_MAPPING_FILE

}

class ConvertPriorityMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.PRIORITY_MAPPING_FILE

}

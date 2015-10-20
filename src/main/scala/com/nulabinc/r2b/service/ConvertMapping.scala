package com.nulabinc.r2b.service

import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.domain.MappingJsonProtocol._
import com.nulabinc.r2b.domain.{Mapping, MappingsWrapper}
import spray.json.JsonParser

import scalax.file.Path

/**
 * @author uchida
 */
trait ConvertMapping {

  def filePath: String

  def convert(target: String): String = {
    val mappings: Seq[Mapping] = wrapper.mappings
    if (mappings.isEmpty) target
    else {
      val mapping: Mapping = mappings.find(_.redmine == target).get
      if (mapping.backlog.nonEmpty) mapping.backlog else target
    }
  }

  private val wrapper: MappingsWrapper = unmarshal(filePath)

  private def unmarshal(strPath: String): MappingsWrapper = {
    val path: Path = Path.fromString(strPath)
    val json = path.lines().mkString
    JsonParser(json).convertTo[MappingsWrapper]
  }

}

class ConvertUserMapping extends ConvertMapping {
  override def filePath: String = ConfigBase.USER_MAPPING_FILE
}

class ConvertStatusMapping extends ConvertMapping {
  override def filePath: String = ConfigBase.STATUS_MAPPING_FILE
}

class ConvertPriorityMapping extends ConvertMapping {
  override def filePath: String = ConfigBase.PRIORITY_MAPPING_FILE
}

package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.r2b.mapping.domain.{Mapping, MappingsWrapper}
import com.nulabinc.backlog.r2b.mapping.domain.MappingJsonProtocol._
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

  val mappings: Seq[Mapping] = unmarshal(filePath)

  private[this] def unmarshal(strPath: String): Seq[Mapping] = {
    val path: Path = Path.fromString(strPath)
    val json       = path.lines().mkString
    JsonParser(json).convertTo[MappingsWrapper].mappings
  }

}

class ConvertUserMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.USER_MAPPING_FILE

  def projectUsers(): Seq[BacklogUser] = {
    mappings.map(toBacklog)
  }

  private[this] def toBacklog(mapping: Mapping): BacklogUser = {
    BacklogUser(optId = None,
                optUserId = Some(mapping.backlog),
                optPassword = None,
                name = mapping.redmine,
                optMailAddress = None,
                roleType = BacklogConstantValue.USER_ROLE)
  }

}

class ConvertStatusMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.STATUS_MAPPING_FILE

}

class ConvertPriorityMapping extends ConvertMapping {

  override def filePath: String = MappingDirectory.PRIORITY_MAPPING_FILE

}

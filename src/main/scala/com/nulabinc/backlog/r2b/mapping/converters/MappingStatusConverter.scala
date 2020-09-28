package com.nulabinc.backlog.r2b.mapping.converters

import com.nulabinc.backlog.migration.common.domain.mappings.ValidatedStatusMapping
import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomStatus,
  BacklogStatus,
  BacklogStatusName
}
import com.nulabinc.backlog.r2b.domain.mappings.{
  RedmineStatusMappingItem,
  ValidatedRedmineStatusMapping
}

object MappingStatusConverter {

  def convert(
      mappings: Seq[ValidatedStatusMapping[RedmineStatusMappingItem]],
      value: String
  ): BacklogStatus =
    if (mappings.isEmpty)
      BacklogCustomStatus.create(BacklogStatusName(value))
    else
      findFromMappings(mappings, value).getOrElse(
        BacklogCustomStatus.create(BacklogStatusName(value))
      )

  def convert(
      mappings: Seq[ValidatedRedmineStatusMapping],
      value: BacklogStatus
  ): BacklogStatus =
    if (mappings.isEmpty) value
    else
      findFromMappings(mappings, value.name.trimmed).getOrElse(value)

  private def findFromMappings(
      mappings: Seq[ValidatedStatusMapping[RedmineStatusMappingItem]],
      value: String
  ): Option[BacklogStatus] =
    for {
      mapping <- mappings.find(_.src.value == value)
    } yield BacklogCustomStatus.create(BacklogStatusName(mapping.dst.value))
}

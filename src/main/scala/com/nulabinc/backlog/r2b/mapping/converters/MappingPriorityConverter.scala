package com.nulabinc.backlog.r2b.mapping.converters

import com.nulabinc.backlog.migration.common.domain.mappings.ValidatedPriorityMapping
import com.nulabinc.backlog.r2b.domain.mappings.RedminePriorityMappingItem

object MappingPriorityConverter {

  def convert(mappings: Seq[ValidatedPriorityMapping[RedminePriorityMappingItem]], value: String): String =
    if (mappings.isEmpty) value
    else findFromMappings(mappings, value).getOrElse(value)

  private def findFromMappings(
      mappings: Seq[ValidatedPriorityMapping[RedminePriorityMappingItem]],
      value: String
  ): Option[String] =
    for {
      mapping <- mappings.find(_.src.value == value)
    } yield mapping.dst.value
}

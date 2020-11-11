package com.nulabinc.backlog.r2b.mapping.converters

import com.nulabinc.backlog.migration.common.domain.mappings.ValidatedUserMapping
import com.nulabinc.backlog.r2b.domain.mappings.RedmineUserMappingItem

object MappingUserConverter {

  def convert(
      mappings: Seq[ValidatedUserMapping[RedmineUserMappingItem]],
      target: String
  ): String =
    if (mappings.isEmpty) target
    else
      mappings.find(_.src.name == target) match {
        case Some(mapping) =>
          if (mapping.dst.value.nonEmpty) mapping.dst.value else target
        case _ =>
          target
      }

}

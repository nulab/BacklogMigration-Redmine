package com.nulabinc.backlog.r2b.codec

import com.nulabinc.backlog.migration.common.codec.{
  PriorityMappingEncoder,
  StatusMappingEncoder,
  UserMappingEncoder
}
import com.nulabinc.backlog.migration.common.domain.mappings.{
  PriorityMapping,
  StatusMapping,
  UserMapping
}
import com.nulabinc.backlog.r2b.domain.mappings.{
  RedminePriorityMappingItem,
  RedmineStatusMappingItem,
  RedmineUserMappingItem
}

object RedmineMappingEncoder {
  implicit val statusEncoder: StatusMappingEncoder[RedmineStatusMappingItem] =
    (mapping: StatusMapping[RedmineStatusMappingItem]) =>
      Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))

  implicit val priorityEncoder: PriorityMappingEncoder[RedminePriorityMappingItem] =
    (mapping: PriorityMapping[RedminePriorityMappingItem]) =>
      Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))

  implicit val userEncoder: UserMappingEncoder[RedmineUserMappingItem] =
    (mapping: UserMapping[RedmineUserMappingItem]) =>
      Seq(
        mapping.src.name,                          // 0
        mapping.src.displayName,                   // 1
        mapping.optDst.map(_.value).getOrElse(""), // 2
        mapping.mappingType                        // 3
      )
}

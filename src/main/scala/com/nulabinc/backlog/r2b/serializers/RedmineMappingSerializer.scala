package com.nulabinc.backlog.r2b.serializers

import com.nulabinc.backlog.migration.common.domain.mappings.{
  PriorityMapping,
  StatusMapping,
  UserMapping
}
import com.nulabinc.backlog.migration.common.serializers.Serializer
import com.nulabinc.backlog.r2b.domain.mappings.{
  RedminePriorityMappingItem,
  RedmineStatusMappingItem,
  RedmineUserMappingItem
}

object RedmineMappingSerializer {
  implicit val statusSerializer: Serializer[StatusMapping[RedmineStatusMappingItem], Seq[String]] =
    (mapping: StatusMapping[RedmineStatusMappingItem]) =>
      Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))

  implicit val prioritySerializer
      : Serializer[PriorityMapping[RedminePriorityMappingItem], Seq[String]] =
    (mapping: PriorityMapping[RedminePriorityMappingItem]) =>
      Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))

  implicit val userSerializer: Serializer[UserMapping[RedmineUserMappingItem], Seq[String]] =
    (mapping: UserMapping[RedmineUserMappingItem]) =>
      Seq(
        mapping.src.name,                          // 0
        mapping.src.displayName,                   // 1
        mapping.optDst.map(_.value).getOrElse(""), // 2
        mapping.mappingType                        // 3
      )
}

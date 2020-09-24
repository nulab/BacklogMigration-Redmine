package com.nulabinc.backlog.r2b.serializers

import com.nulabinc.backlog.migration.common.domain.mappings.{PriorityMapping, StatusMapping}
import com.nulabinc.backlog.migration.common.serializers.Serializer
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem}

object RedmineMappingSerializer {
  implicit val statusSerializer: Serializer[StatusMapping[RedmineStatusMappingItem], Seq[String]] =
    (mapping: StatusMapping[RedmineStatusMappingItem]) => Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))

  implicit val prioritySerializer: Serializer[PriorityMapping[RedminePriorityMappingItem], Seq[String]] =
    (mapping: PriorityMapping[RedminePriorityMappingItem]) => Seq(mapping.src.value, mapping.optDst.map(_.value).getOrElse(""))
}

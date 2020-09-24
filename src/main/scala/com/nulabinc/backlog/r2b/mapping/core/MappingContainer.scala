package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.migration.common.domain.mappings.{ValidatedPriorityMapping, ValidatedStatusMapping}
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem}
import com.nulabinc.backlog.r2b.mapping.domain.Mapping

case class MappingContainer(
    user: Seq[Mapping],
    priority: Seq[ValidatedPriorityMapping[RedminePriorityMappingItem]],
    statuses: Seq[ValidatedStatusMapping[RedmineStatusMappingItem]]
)

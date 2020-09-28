package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.migration.common.domain.mappings.{ValidatedPriorityMapping, ValidatedStatusMapping, ValidatedUserMapping}
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem, RedmineUserMappingItem}

case class MappingContainer(
    user: Seq[ValidatedUserMapping[RedmineUserMappingItem]],
    priority: Seq[ValidatedPriorityMapping[RedminePriorityMappingItem]],
    statuses: Seq[ValidatedStatusMapping[RedmineStatusMappingItem]]
)

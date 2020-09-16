package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.migration.common.domain.mappings.ValidatedStatusMapping
import com.nulabinc.backlog.r2b.domain.mappings.RedmineStatusMappingItem
import com.nulabinc.backlog.r2b.mapping.domain.Mapping

case class MappingContainer(
    user: Seq[Mapping],
    priority: Seq[Mapping],
    statuses: Seq[ValidatedStatusMapping[RedmineStatusMappingItem]]
)

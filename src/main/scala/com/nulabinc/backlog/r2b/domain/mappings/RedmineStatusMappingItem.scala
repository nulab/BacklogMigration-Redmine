package com.nulabinc.backlog.r2b.domain.mappings

import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogStatusMappingItem, ValidatedStatusMapping}

case class RedmineStatusMappingItem(value: String)

case class ValidatedRedmineStatusMapping(
    src: RedmineStatusMappingItem,
    dst: BacklogStatusMappingItem
) extends ValidatedStatusMapping[RedmineStatusMappingItem] {
  override val srcDisplayValue: String = src.value
}

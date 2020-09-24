package com.nulabinc.backlog.r2b.domain.mappings

import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogPriorityMappingItem, ValidatedPriorityMapping}

case class RedminePriorityMappingItem(value: String)

case class ValidatedRedminePriorityMapping(
    src: RedminePriorityMappingItem,
    dst: BacklogPriorityMappingItem
) extends ValidatedPriorityMapping[RedminePriorityMappingItem] {
  override val srcDisplayValue: String = src.value
}

object ValidatedRedminePriorityMapping {
  def from(
      mapping: ValidatedPriorityMapping[RedminePriorityMappingItem]
  ): ValidatedRedminePriorityMapping =
    ValidatedRedminePriorityMapping(src = mapping.src, dst = mapping.dst)
}

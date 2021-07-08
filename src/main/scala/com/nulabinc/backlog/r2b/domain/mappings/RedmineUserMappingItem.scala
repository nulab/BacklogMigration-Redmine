package com.nulabinc.backlog.r2b.domain.mappings

import com.nulabinc.backlog.migration.common.domain.mappings.{
  BacklogUserMappingItem,
  ValidatedUserMapping
}

case class RedmineUserMappingItem(name: String, displayName: String)

case class ValidatedRedmineUserMapping(
    src: RedmineUserMappingItem,
    dst: BacklogUserMappingItem
) extends ValidatedUserMapping[RedmineUserMappingItem] {
  override val srcDisplayValue: String = src.displayName
}

object ValidatedRedmineUserMapping {
  def from(
      mapping: ValidatedUserMapping[RedmineUserMappingItem]
  ): ValidatedRedmineUserMapping =
    ValidatedRedmineUserMapping(src = mapping.src, dst = mapping.dst)
}

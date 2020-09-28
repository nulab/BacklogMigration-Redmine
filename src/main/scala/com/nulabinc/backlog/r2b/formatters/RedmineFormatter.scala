package com.nulabinc.backlog.r2b.formatters

import com.nulabinc.backlog.migration.common.domain.mappings.{
  PriorityMapping,
  StatusMapping,
  UserMapping
}
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.r2b.domain.mappings.{
  RedminePriorityMappingItem,
  RedmineStatusMappingItem,
  RedmineUserMappingItem
}

object RedmineFormatter {

  implicit object StatusFormatter extends Formatter[StatusMapping[RedmineStatusMappingItem]] {
    def format(value: StatusMapping[RedmineStatusMappingItem]): (String, String) =
      (value.src.value, value.optDst.map(_.value).getOrElse(""))
  }

  implicit object PriorityFormatter extends Formatter[PriorityMapping[RedminePriorityMappingItem]] {
    def format(
        value: PriorityMapping[RedminePriorityMappingItem]
    ): (String, String) =
      (value.src.value, value.optDst.map(_.value).getOrElse(""))
  }

  implicit object UserFormatter extends Formatter[UserMapping[RedmineUserMappingItem]] {
    def format(value: UserMapping[RedmineUserMappingItem]): (String, String) =
      (value.src.displayName, value.optDst.map(_.value).getOrElse(""))
  }
}

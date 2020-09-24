package com.nulabinc.backlog.r2b.formatters

import com.nulabinc.backlog.migration.common.domain.mappings.StatusMapping
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.nulabinc.backlog.r2b.domain.mappings.RedmineStatusMappingItem

object RedmineFormatter {

  implicit object StatusFormatter extends Formatter[StatusMapping[RedmineStatusMappingItem]] {
    def format(value: StatusMapping[RedmineStatusMappingItem]): (String, String) =
      (value.src.value, value.optDst.map(_.value).getOrElse(""))
  }
}

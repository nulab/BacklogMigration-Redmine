package com.nulabinc.backlog.r2b.mapping

import com.nulabinc.backlog.migration.common.domain.mappings.{MappingHeader, StatusMapping}

object RedmineMappingHeader {

  implicit object StatusMappingHeader extends MappingHeader[StatusMapping[_]] {
    val headers: Seq[String] = Seq("Redmine", "Backlog")
  }

}

package com.nulabinc.backlog.r2b.mapping

import com.nulabinc.backlog.migration.common.domain.mappings.{
  MappingHeader,
  PriorityMapping,
  StatusMapping,
  UserMapping
}

object RedmineMappingHeader {

  implicit object StatusMappingHeader extends MappingHeader[StatusMapping[_]] {
    val headers: Seq[String] = Seq("Redmine", "Backlog")
  }

  implicit object PriorityMappingHeader extends MappingHeader[PriorityMapping[_]] {
    val headers: Seq[String] = Seq("Redmine", "Backlog")
  }

  implicit object UserMappingHeader extends MappingHeader[UserMapping[_]] {
    val headers: Seq[String] = Seq(
      "Redmine user name",
      "Redmine user display name",
      "Backlog user name"
    )
  }

}

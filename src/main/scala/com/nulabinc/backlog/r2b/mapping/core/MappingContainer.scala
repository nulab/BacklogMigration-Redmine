package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.r2b.mapping.domain.Mapping

case class MappingContainer(user: Seq[Mapping], priority: Seq[Mapping], status: Seq[Mapping])

package com.nulabinc.backlog.r2b.mapping.service

import com.nulabinc.backlog.r2b.mapping.domain.Mapping

class MappingStatusServiceImpl(mappings: Seq[Mapping]) extends MappingStatusService with MappingConverter {

  override def convert(value: String) = convert(mappings, value)

}

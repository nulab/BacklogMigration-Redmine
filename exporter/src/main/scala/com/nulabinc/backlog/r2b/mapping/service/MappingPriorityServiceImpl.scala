package com.nulabinc.backlog.r2b.mapping.service

import com.nulabinc.backlog.r2b.mapping.domain.Mapping

class MappingPriorityServiceImpl(mappings: Seq[Mapping]) extends MappingPriorityService with MappingConverter {

  override def convert(value: String) = convert(mappings, value)

}

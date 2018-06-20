package com.nulabinc.backlog.r2b.mapping.service

import com.nulabinc.backlog.r2b.mapping.domain.Mapping

class MappingUserServiceImpl(mappings: Seq[Mapping]) extends MappingUserService with MappingConverter {

  override def convert(value: String) = convert(mappings, value)

}

package com.nulabinc.r2b.mapping.domain

import spray.json.DefaultJsonProtocol

/**
  * @author uchida
  */
case class MappingsWrapper(description: String, mappings: Seq[Mapping])

case class Mapping(redmine: String, backlog: String)

case class MappingItem(name: String, display: String)

object MappingJsonProtocol extends DefaultJsonProtocol {
  implicit val MappingFormat         = jsonFormat2(Mapping)
  implicit val MappingsWrapperFormat = jsonFormat2(MappingsWrapper)
}

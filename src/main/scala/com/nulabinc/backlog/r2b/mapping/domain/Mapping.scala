package com.nulabinc.backlog.r2b.mapping.domain

import spray.json.DefaultJsonProtocol

/**
 * @author
 *   uchida
 */
case class MappingsWrapper(description: String, mappings: Seq[Mapping])

case class Mapping(redmine: String, backlog: String)

object MappingJsonProtocol extends DefaultJsonProtocol {
  implicit val MappingFormat         = jsonFormat2(Mapping)
  implicit val MappingsWrapperFormat = jsonFormat2(MappingsWrapper)
}

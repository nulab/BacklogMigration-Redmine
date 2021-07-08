package com.nulabinc.backlog.r2b.redmine.domain

import com.nulabinc.backlog.migration.common.domain.support.{
  Identifier,
  Undefined
}

/**
  * @author uchida
  */
case class RedmineCustomFieldDefinition(
    id: Int,
    name: String,
    customizedType: String,
    fieldFormat: String,
    optRegexp: Option[String],
    optMinLength: Option[Int],
    optMaxLength: Option[Int],
    isRequired: Boolean,
    isMultiple: Boolean,
    optDefaultValue: Option[String],
    trackers: Seq[RedmineTracker],
    possibleValues: Seq[String]
)

case class RedmineTracker(id: Int, name: String)

class RedmineProjectId(projectId: Int) extends Identifier[Int] {

  def value = projectId

}
object RedmineProjectId {
  val undefined = new RedmineProjectId(0) with Undefined

  def apply(value: Int): RedmineProjectId = new RedmineProjectId(value)
}

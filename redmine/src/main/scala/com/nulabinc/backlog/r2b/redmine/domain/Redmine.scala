package com.nulabinc.backlog.r2b.redmine.domain

/**
  * @author uchida
  */
case class RedmineCustomFieldDefinition(id: Int,
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
                                        possibleValues: Seq[String])

case class RedmineTracker(id: Int, name: String)

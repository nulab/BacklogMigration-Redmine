package com.nulabinc.backlog.r2b.redmine.service

import com.nulabinc.backlog.r2b.redmine.domain.RedmineCustomFieldDefinition

/**
  * @author uchida
  */
trait CustomFieldService {

  def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition]

}

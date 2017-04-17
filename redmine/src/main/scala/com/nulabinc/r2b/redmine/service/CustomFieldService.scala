package com.nulabinc.r2b.redmine.service

import com.nulabinc.r2b.redmine.domain.RedmineCustomFieldDefinition

/**
  * @author uchida
  */
trait CustomFieldService {

  def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition]

}

package com.nulabinc.r2b.service

import com.nulabinc.r2b.domain.RedmineCustomFieldDefinition

/**
  * @author uchida
  */
trait CustomFieldService {

  def allCustomFieldDefinitions(): Seq[RedmineCustomFieldDefinition]

}

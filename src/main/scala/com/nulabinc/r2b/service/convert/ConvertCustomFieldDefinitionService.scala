package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.migration.domain.BacklogCustomFieldDefinition
import com.nulabinc.r2b.domain.RedmineCustomFieldDefinition

/**
  * @author uchida
  */
trait ConvertCustomFieldDefinitionService {

  def convert(redmineCustomFieldDefinition: RedmineCustomFieldDefinition): BacklogCustomFieldDefinition

}

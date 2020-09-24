package com.nulabinc.backlog.r2b.exporter.core

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.domain.BacklogProjectKey
import com.nulabinc.backlog.r2b.exporter.conf.ExportConfig
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.service.MappingUserService
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineProjectId}
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, ProjectService, WikiService}

class ExportContextProvider @Inject() (
    apiConfig: RedmineApiConfiguration,
    backlogProjectKey: BacklogProjectKey,
    exportConfig: ExportConfig,
    projectId: RedmineProjectId,
    propertyValue: PropertyValue,
    backlogPaths: BacklogPaths,
    issueService: IssueService,
    projectService: ProjectService,
    mappingUserService: MappingUserService,
    mappingContainer: MappingContainer,
    wikiService: WikiService,
    issueWrites: IssueWrites,
    journalWrites: JournalWrites,
    userWrites: UserWrites,
    customFieldWrites: CustomFieldWrites,
    customFieldValueWrites: CustomFieldValueWrites,
    attachmentWrites: AttachmentWrites,
    wikiWrites: WikiWrites
) {

  def get(): ExportContext = {
    ExportContext(
      apiConfig,
      backlogProjectKey,
      exportConfig,
      projectId,
      backlogPaths,
      propertyValue,
      mappingUserService,
      mappingContainer,
      projectService,
      issueService,
      wikiService,
      issueWrites,
      journalWrites,
      userWrites,
      customFieldWrites,
      customFieldValueWrites,
      attachmentWrites,
      wikiWrites
    )
  }

}

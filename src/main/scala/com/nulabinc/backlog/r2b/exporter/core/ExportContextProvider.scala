package com.nulabinc.backlog.r2b.exporter.core

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.exporter.conf.ExportConfig
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.service.{MappingPriorityService, MappingUserService}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineProjectId}
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, ProjectService, WikiService}

class ExportContextProvider @Inject()(apiConfig: RedmineApiConfiguration,
                                      exportConfig: ExportConfig,
                                      projectId: RedmineProjectId,
                                      propertyValue: PropertyValue,
                                      backlogPaths: BacklogPaths,
                                      issueService: IssueService,
                                      projectService: ProjectService,
                                      mappingUserService: MappingUserService,
                                      mappingPriorityService: MappingPriorityService,
                                      wikiService: WikiService,
                                      issueWrites: IssueWrites,
                                      journalWrites: JournalWrites,
                                      userWrites: UserWrites,
                                      customFieldWrites: CustomFieldWrites,
                                      customFieldValueWrites: CustomFieldValueWrites,
                                      attachmentWrites: AttachmentWrites,
                                      wikiWrites: WikiWrites) {

  def get(): ExportContext = {
    ExportContext(apiConfig,
                  exportConfig,
                  projectId,
                  backlogPaths,
                  propertyValue,
                  mappingUserService,
                  mappingPriorityService,
                  projectService,
                  issueService,
                  wikiService,
                  issueWrites,
                  journalWrites,
                  userWrites,
                  customFieldWrites,
                  customFieldValueWrites,
                  attachmentWrites,
                  wikiWrites)
  }

}

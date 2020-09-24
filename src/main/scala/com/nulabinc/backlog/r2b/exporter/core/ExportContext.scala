package com.nulabinc.backlog.r2b.exporter.core

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.domain.BacklogProjectKey
import com.nulabinc.backlog.r2b.exporter.conf.ExportConfig
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.service.MappingUserService
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineProjectId}
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, ProjectService, WikiService}

case class ExportContext(
    apiConfig: RedmineApiConfiguration,
    backlogProjectKey: BacklogProjectKey,
    exportConfig: ExportConfig,
    projectId: RedmineProjectId,
    backlogPaths: BacklogPaths,
    propertyValue: PropertyValue,
    mappingUserService: MappingUserService,
    mappingContainer: MappingContainer,
    projectService: ProjectService,
    issueService: IssueService,
    wikiService: WikiService,
    issueWrites: IssueWrites,
    journalWrites: JournalWrites,
    userWrites: UserWrites,
    customFieldWrites: CustomFieldWrites,
    customFieldValueWrites: CustomFieldValueWrites,
    attachmentWrites: AttachmentWrites,
    wikiWrites: WikiWrites
)

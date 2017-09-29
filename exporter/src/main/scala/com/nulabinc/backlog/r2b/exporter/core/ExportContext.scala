package com.nulabinc.backlog.r2b.exporter.core

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.service.{MappingPriorityService, MappingUserService}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.{PropertyValue, RedmineProjectId}
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, ProjectService, WikiService}

case class ExportContext(apiConfig: RedmineApiConfiguration,
                         projectId: RedmineProjectId,
                         backlogPaths: BacklogPaths,
                         propertyValue: PropertyValue,
                         mappingUserService: MappingUserService,
                         mappingPriorityService: MappingPriorityService,
                         projectService: ProjectService,
                         issueService: IssueService,
                         wikiService: WikiService,
                         issueWrites: IssueWrites,
                         journalWrites: JournalWrites,
                         userWrites: UserWrites,
                         customFieldWrites: CustomFieldWrites,
                         customFieldValueWrites: CustomFieldValueWrites,
                         attachmentWrites: AttachmentWrites,
                         wikiWrites: WikiWrites)

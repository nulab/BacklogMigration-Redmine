package com.nulabinc.backlog.r2b.exporter.core

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.service.{MappingPriorityService, MappingUserService}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue

case class ExportContext(mappingUserService: MappingUserService,
                         mappingPriorityService: MappingPriorityService,
                         apiConfig: RedmineApiConfiguration,
                         backlogPaths: BacklogPaths,
                         propertyValue: PropertyValue,
                         issueWrites: IssueWrites,
                         journalWrites: JournalWrites,
                         userWrites: UserWrites,
                         customFieldWrites: CustomFieldWrites,
                         customFieldValueWrites: CustomFieldValueWrites,
                         attachmentWrites: AttachmentWrites)

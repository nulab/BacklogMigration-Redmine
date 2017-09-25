package com.nulabinc.backlog.r2b.exporter.core

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.core.{ConvertPriorityMapping, ConvertUserMapping}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue

case class ExportContext(userMapping: ConvertUserMapping,
                         priorityMapping: ConvertPriorityMapping,
                         apiConfig: RedmineApiConfiguration,
                         backlogPaths: BacklogPaths,
                         propertyValue: PropertyValue,
                         issueWrites: IssueWrites,
                         journalWrites: JournalWrites,
                         userWrites: UserWrites,
                         customFieldWrites: CustomFieldWrites,
                         customFieldValueWrites: CustomFieldValueWrites,
                         attachmentWrites: AttachmentWrites)

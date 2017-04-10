package com.nulabinc.r2b.exporter.convert

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogProject
import com.nulabinc.backlog4j.Project.TextFormattingRule
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
class ProjectWrites @Inject()(@Named("backlogProjectKey") backlogProjectKey: String) extends Writes[Project, BacklogProject] {

  override def writes(project: Project): BacklogProject = {
    BacklogProject(optId = Some(project.getId.intValue()),
                   name = project.getName,
                   key = backlogProjectKey,
                   isChartEnabled = true,
                   isSubtaskingEnabled = true,
                   textFormattingRule = TextFormattingRule.Markdown.getStrValue)
  }

}

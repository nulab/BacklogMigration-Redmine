package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.{BacklogProject, BacklogProjectKey}
import com.nulabinc.backlog4j.Project.TextFormattingRule
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
private[exporter] class ProjectWrites @Inject()(projectKey: BacklogProjectKey) extends Writes[Project, BacklogProject] {

  override def writes(project: Project): BacklogProject = {
    BacklogProject(optId = Some(project.getId.intValue()),
                   name = project.getName,
                   key = projectKey.value,
                   isChartEnabled = true,
                   isSubtaskingEnabled = true,
                   textFormattingRule = TextFormattingRule.Markdown.getStrValue)
  }

}

package com.nulabinc.r2b.core

import com.nulabinc.backlog.importer.actor.backlog.BacklogActor
import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.{BacklogClient, Project => BacklogProject}
import com.nulabinc.r2b.actor.convert.ConvertActor
import com.nulabinc.r2b.actor.redmine.RedmineActor
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.helper.R2BSimpleFixture
import com.taskadapter.redmineapi.bean.{Project => RedmineProject}
import org.specs2.mutable.Specification
import org.specs2.specification.{AfterExample, BeforeExample}

/**
  * @author uchida
  */
class R2BSpec extends Specification with BeforeExample with AfterExample with R2BSimpleFixture {

  def before = {
    val conf: R2BConfig = getConfig
    RedmineActor(conf)
    ConvertActor(conf)
    BacklogActor(BacklogConfig(conf.backlogUrl, conf.backlogKey))
  }

  def after = {
    val conf: R2BConfig = getConfig
    val backlog: BacklogClient = getBacklogClient(conf)
    backlog.deleteProject(conf.projects(0).getBacklogKey())
  }

  "Project must match" in new R2BSimpleFixture {
    val redmineProject: RedmineProject = redmine.getProjectManager.getProjectByKey(conf.projects(0).redmine)
    val backlogProject: BacklogProject = backlog.getProject(conf.projects(0).getBacklogKey())

    "Project must match" in {
      backlogProject.getName must be_==(redmineProject.getName)
      backlogProject.isChartEnabled must be_==(true)
      backlogProject.isSubtaskingEnabled must be_==(true)
      backlogProject.getTextFormattingRule must be_==(BacklogProject.TextFormattingRule.Backlog)
    }
  }

}

package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.importer.domain.{BacklogProject, BacklogProjectsWrapper}
import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.domain.RedmineProject

/**
  * @author uchida
  */
object ConvertProjects {

  def apply(redmineProjects: Seq[RedmineProject], projectKeys: Seq[ParamProjectKey]): BacklogProjectsWrapper = {
    val backlogProjects: Seq[BacklogProject] = redmineProjects.map(project => getBacklogProject(project, projectKeys))
    BacklogProjectsWrapper(backlogProjects)
  }

  private def getBacklogProject(redmineProject: RedmineProject, projectKeys: Seq[ParamProjectKey]): BacklogProject =
    BacklogProject(
      id = redmineProject.id,
      name = redmineProject.name,
      key = convertProjectKey(redmineProject.identifier, projectKeys))

  private def convertProjectKey(redmineIdentifier: String, paramProjectKeys: Seq[ParamProjectKey]): String = {
    val paramProjectKey: Option[ParamProjectKey] = paramProjectKeys.find(projectKey => projectKey.redmine == redmineIdentifier)
    paramProjectKey match {
      case Some(projectKey) => projectKey.getBacklogKey()
      case None => redmineIdentifier.toUpperCase.replaceAll("-", "_")
    }
  }

}
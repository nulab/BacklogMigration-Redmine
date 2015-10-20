package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.BacklogService
import com.osinka.i18n.Messages

/**
 * @author uchida
 */
class ExecuteCommand(r2bConf: R2BConfig) extends CommonCommand {

  val mappingService: MappingService = load(r2bConf)

  def check(): Boolean =
    if (isAllExists) {
      if (isAllParsed) {
        isAllValid
      } else false
    } else {
      printlog()
      printlog(Messages("invalid_setup"))
      false
    }

  def confirm(useProjects: Seq[ParamProjectKey]): Boolean = {
    printlog()
    showProjectsMapping(useProjects)
    mappingService.user.show()
    mappingService.status.show()
    mappingService.priority.show()
    printlog()
    val input: String = scala.io.StdIn.readLine(Messages("mapping.confirm"))
    input == "y" || input == "Y"
  }

  def useProjectsConfirm(): Seq[ParamProjectKey] = r2bConf.projects.flatMap(confirmUseProject)

  private def confirmUseProject(projectKey: ParamProjectKey): Option[ParamProjectKey] = {
    val backlogService: BacklogService = new BacklogService(BacklogConfig(r2bConf.backlogUrl, r2bConf.backlogKey))
    if (backlogService.getProject(projectKey.getBacklogKey()).isRight) {
      printlog()
      val input: String = scala.io.StdIn.readLine(Messages("message.backlog_project_already_exist", projectKey.getBacklogKey()))
      if (input == "y" || input == "Y") Some(projectKey)
      else None
    } else Some(projectKey)
  }

  private def isAllExists: Boolean =
    mappingService.user.isExists && mappingService.status.isExists && mappingService.priority.isExists

  private def isAllParsed: Boolean = {
    mappingService.user.showBrokenFileMessage()
    mappingService.status.showBrokenFileMessage()
    mappingService.priority.showBrokenFileMessage()

    mappingService.user.isParsed && mappingService.status.isParsed && mappingService.priority.isParsed
  }

  private def isAllValid: Boolean = {
    mappingService.user.showInvalidErrors()
    mappingService.status.showInvalidErrors()
    mappingService.priority.showInvalidErrors()

    mappingService.user.isValid && mappingService.status.isValid && mappingService.priority.isValid
  }


  private def showProjectsMapping(useProjects: Seq[ParamProjectKey]) = {
    printlog(Messages("mapping.show", Messages("projects")))
    printlog("--------------------------------------------------")

    useProjects.foreach(showMapping)

    printlog("--------------------------------------------------")
    printlog()
  }

  private def showMapping(paramProjectKey: ParamProjectKey) = {
    val backlogDisplay: String =
      if (paramProjectKey.backlog.isDefined) paramProjectKey.backlog.get
      else paramProjectKey.redmine.toUpperCase.replaceAll("-", "_")
    printlog("- " + paramProjectKey.redmine + " => " + backlogDisplay)
  }


}

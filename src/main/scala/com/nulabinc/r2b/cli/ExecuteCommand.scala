package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.BacklogService
import com.osinka.i18n.Messages

/**
 * @author uchida
 */
class ExecuteCommand(conf: R2BConfig) extends CommonCommand {

  val mappingService: MappingService = load(conf)

  def check(): Boolean =
    if (isAllExists) {
      if (isAllParsed) {
        isAllValid
      } else false
    } else {
      invalidMessage()
      false
    }

  def confirm(useProjects: Seq[ParamProjectKey]): Boolean = {
    
    newLine()
    
    showProjectsMapping(useProjects)
    mappingService.user.show()
    mappingService.status.show()
    mappingService.priority.show()
    
    newLine()
    
    val input: String = scala.io.StdIn.readLine(Messages("mapping.confirm"))
    input == "y" || input == "Y"
  }

  def useProjectsConfirm(): Seq[ParamProjectKey] = conf.projects.flatMap(confirmUseProject)

  private def confirmUseProject(projectKey: ParamProjectKey): Option[ParamProjectKey] = {
    val backlogService: BacklogService = new BacklogService(BacklogConfig(conf.backlogUrl, conf.backlogKey))
    if (backlogService.getProject(projectKey.getBacklogKey()).isRight) {

      newLine()

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
    title(Messages("mapping.show", Messages("projects")),TOP)

    useProjects.foreach(showMapping)

    separatorln()
  }

  private def showMapping(paramProjectKey: ParamProjectKey) = {
    val backlogDisplay: String =
      if (paramProjectKey.backlog.isDefined) paramProjectKey.backlog.get
      else paramProjectKey.redmine.toUpperCase.replaceAll("-", "_")
    info("- " + paramProjectKey.redmine + " => " + backlogDisplay)
  }

  private def invalidMessage() = {
    newLine()
    info(Messages("invalid_setup"))
  }

}

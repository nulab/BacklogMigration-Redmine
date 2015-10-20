package com.nulabinc.r2b.cli

import com.nulabinc.r2b.conf.R2BConfig
import com.osinka.i18n.Messages

/**
 * @author uchida
 */
class InitCommand(r2bConf: R2BConfig) extends CommonCommand {

  val mappingService: MappingService = load(r2bConf)

  def execute() = {
    printlog()
    mappingService.user.createExec()
    mappingService.priority.createExec()
    mappingService.status.createExec()
    printlog()
    printlog(Messages("mapping.confirm_fix"))
  }

}

package com.nulabinc.r2b.cli

import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig

/**
 * @author uchida
 */
trait CommonCommand extends R2BLogging {

  def load(r2bConf: R2BConfig): MappingService = {

    newLine()

    val userMapping: UserMapping = new UserMapping(r2bConf)
    val statusMapping: StatusMapping = new StatusMapping(r2bConf)
    val priorityMapping: PriorityMapping = new PriorityMapping(r2bConf)
    MappingService(userMapping, statusMapping, priorityMapping)
  }

  case class MappingService(user: UserMapping,
                            status: StatusMapping,
                            priority: PriorityMapping)

}

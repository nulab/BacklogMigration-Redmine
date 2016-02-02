package com.nulabinc.r2b.cli

import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.R2BConfig

/**
 * @author uchida
 */
trait CommonCommand extends R2BLogging {

  def load(conf: R2BConfig): MappingService = {

    newLine()

    val userMapping: UserMapping = new UserMapping(conf)
    val statusMapping: StatusMapping = new StatusMapping(conf)
    val priorityMapping: PriorityMapping = new PriorityMapping(conf)
    MappingService(userMapping, statusMapping, priorityMapping)
  }

  case class MappingService(user: UserMapping,
                            status: StatusMapping,
                            priority: PriorityMapping)

}

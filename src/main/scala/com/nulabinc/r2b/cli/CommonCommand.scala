package com.nulabinc.r2b.cli

import java.util.Locale

import com.nulabinc.r2b.actor.utils.Logging
import com.nulabinc.r2b.conf.R2BConfig
import com.osinka.i18n.Lang

/**
 * @author uchida
 */
trait CommonCommand extends Logging {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def load(r2bConf: R2BConfig): MappingService = {
    printlog()
    val userMapping: UserMapping = new UserMapping(r2bConf)
    val statusMapping: StatusMapping = new StatusMapping(r2bConf)
    val priorityMapping: PriorityMapping = new PriorityMapping(r2bConf)
    MappingService(userMapping, statusMapping, priorityMapping)
  }

  case class MappingService(user: UserMapping,
                            status: StatusMapping,
                            priority: PriorityMapping)

}

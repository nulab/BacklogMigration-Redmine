package com.nulabinc.r2b.actor.utils

import org.slf4j.{LoggerFactory, Logger}

/**
 * @author uchida
 */
trait Logging {

  val log: Logger = LoggerFactory.getLogger(getClass)

}

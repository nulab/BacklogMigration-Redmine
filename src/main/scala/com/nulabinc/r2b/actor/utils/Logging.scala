package com.nulabinc.r2b.actor.utils

import org.slf4j.{LoggerFactory, Logger}

/**
 * @author uchida
 */
trait Logging {

  val log: Logger = LoggerFactory.getLogger("CommonCommand")

  def printlog() = {
    println("")
    log.info("")
  }

  def printlog(message: String) = {
    println(message)
    log.info(message)
  }

}

package com.nulabinc.r2b.cli

/**
 * @author uchida
 */
class ParamProjectKey(unconverted: String) {

  val keys: Array[String] = unconverted.split(":")
  val redmine: String = if (keys.size == 2) keys(0) else keys(0)
  val backlog: Option[String] = if (keys.size == 2) Some(keys(1)) else None

  def getBacklogKey(): String = backlog.getOrElse(redmine.toUpperCase.replaceAll("-", "_"))

}
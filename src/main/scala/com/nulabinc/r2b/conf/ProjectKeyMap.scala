package com.nulabinc.r2b.conf

/**
  * @author uchida
  */
class ProjectKeyMap(unconverted: String) {

  val keys: Array[String] = unconverted.split(":")
  val redmine: String = keys(0)
  val backlog: Option[String] = if (keys.length == 2) Some(keys(1)) else None

  def getBacklogKey(): String = backlog.getOrElse(redmine.toUpperCase.replaceAll("-", "_"))

}
package com.nulabinc.backlog.r2b.utils

/**
 * @author
 *   uchida
 */
object ClassVersion {

  private[this] val CLASS_VERSION_8: Double = 52.0
  private[this] val CLASS_VERSION_9: Double = 53.0

  def isValid(): Boolean = {
    val v = System.getProperty("java.class.version").toDouble
    v >= CLASS_VERSION_8 && v < CLASS_VERSION_9
  }

}

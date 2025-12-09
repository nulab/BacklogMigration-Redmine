package com.nulabinc.backlog.r2b.utils

/**
 * @author
 *   uchida
 */
object ClassVersion {

  private[this] val CLASS_VERSION_11: Double = 55.0
  private[this] val CLASS_VERSION_12: Double = 56.0

  def isValid(): Boolean = {
    val v = System.getProperty("java.class.version").toDouble
    v >= CLASS_VERSION_11 && v < CLASS_VERSION_12
  }

}

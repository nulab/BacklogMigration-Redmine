package com.nulabinc.r2b.utils

/**
  * @author uchida
  */
object ClassVersion {

  private val CLASS_VERSION_8: Double = 52.0;

  def isValid(): Boolean = System.getProperty("java.class.version").toDouble >= CLASS_VERSION_8

}

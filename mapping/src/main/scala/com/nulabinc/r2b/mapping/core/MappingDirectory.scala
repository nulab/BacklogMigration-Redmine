package com.nulabinc.r2b.mapping.core

import java.io.File

/**
  * @author uchida
  */
object MappingDirectory {

  private[this] val WORKING_DIRECTORY = new File(".").getAbsoluteFile.getParent
  val ROOT                            = WORKING_DIRECTORY + "/mapping"
  val USER_MAPPING_FILE               = ROOT + "/users.json"
  val STATUS_MAPPING_FILE             = ROOT + "/statuses.json"
  val PRIORITY_MAPPING_FILE           = ROOT + "/priorities.json"

}

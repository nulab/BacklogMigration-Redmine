package com.nulabinc.backlog.r2b.mapping.core

import java.io.File
import java.nio.file.Path

object MappingDirectory {

  private val WORKING_DIRECTORY = new File(".").getAbsoluteFile.getParent
  private val ROOT              = WORKING_DIRECTORY + "/mapping"

  final val USER_MAPPING_FILE: String          = ROOT + "/users.json"
  final val STATUS_MAPPING_FILE: Path          = toAbsolutePath(ROOT + "/statuses.csv")
  final val PRIORITY_MAPPING_FILE: String      = ROOT + "/priorities.json"
  final val USER_MAPPING_LIST_FILE: String     = ROOT + "/users_list.csv"
  final val STATUS_MAPPING_LIST_FILE: Path     = toAbsolutePath(ROOT + "/statuses_list.csv")
  final val PRIORITY_MAPPING_LIST_FILE: String = ROOT + "/priorities_list.csv"

  private def toAbsolutePath(str: String): Path =
    new File(str).getAbsoluteFile.toPath
}

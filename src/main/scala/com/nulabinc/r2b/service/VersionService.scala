package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.Version

/**
  * @author uchida
  */
trait VersionService {

  def allVersions(): Seq[Version]

}

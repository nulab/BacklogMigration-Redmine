package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.bean.Version

/**
  * @author uchida
  */
trait VersionService {

  def allVersions(): Seq[Version]

}

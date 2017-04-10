package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.Project

/**
  * @author uchida
  */
trait ProjectService {

  def project(): Project

  def optProject(): Option[Project]

}

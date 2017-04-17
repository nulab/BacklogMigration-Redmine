package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.Project

/**
  * @author uchida
  */
trait ProjectService {

  def tryProjectOfKey(projectKey: String): Project

  def optProjectOfId(id: Int): Option[Project]

  def optProjectOfKey(projectKey: String): Option[Project]

}

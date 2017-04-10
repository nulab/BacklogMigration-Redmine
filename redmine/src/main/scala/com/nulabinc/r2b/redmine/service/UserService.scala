package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
trait UserService {

  def allUsers(): Seq[User]

  def optUserOfId(id: Int): Option[User]

}

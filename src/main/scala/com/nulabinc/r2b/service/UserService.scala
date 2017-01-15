package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
trait UserService {

  def allUsers(): Seq[User]

  def optUserOfId(id: Int): Option[User]

}

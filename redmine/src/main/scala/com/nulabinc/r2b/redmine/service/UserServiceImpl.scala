package com.nulabinc.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.User

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class UserServiceImpl @Inject()(redmine: RedmineManager) extends UserService with Logging {

  override def allUsers(): Seq[User] = {
    val users: Seq[User] = redmine.getUserManager.getUsers.asScala
    users.flatMap(user => optUserOfId(user.getId))
  }

  override def tryUserOfId(id: Int): User =
    redmine.getUserManager.getUserById(id)

  override def optUserOfId(id: Int): Option[User] = {
    try {
      Some(redmine.getUserManager.getUserById(id))
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        None
    }
  }

}

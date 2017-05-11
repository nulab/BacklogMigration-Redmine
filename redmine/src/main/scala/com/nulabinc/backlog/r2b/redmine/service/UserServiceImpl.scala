package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.User

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class UserServiceImpl @Inject()(redmine: RedmineManager) extends UserService with Logging {

  override def allUsers(): Seq[User] = {
    def addInfo(user: User): Option[User] = {
      (Option(user.getLogin), Option(user.getFullName)) match {
        case (Some(_), Some(_)) => Some(user)
        case _                  => optUserOfId(user.getId)
      }
    }

    val users: Seq[User] = redmine.getUserManager.getUsers.asScala
    users.flatMap(addInfo)
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

package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogUserData(id: Long, userId: String)

class BacklogUsers(tag: Tag) extends Table[BacklogUserData](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey)

  def userId = column[String]("user_id", O.NotNull)

  def * = (id, userId) <>(BacklogUserData.tupled, BacklogUserData.unapply)
}

object BacklogUserDao {

  def create(e: BacklogUserData) = database.withTransaction { implicit session: Session =>
    users.insert(e)
  }

  def createAll(users: Seq[BacklogUserData]) = database.withTransaction { implicit session: Session =>
    users.foreach(create)
  }

  def findByUserId(userId: String): Option[BacklogUserData] = database.withTransaction { implicit session: Session =>
    users.filter(_.userId === userId).firstOption
  }

}
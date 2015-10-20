package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogProjectData(id: Long, projectKey: String)

class BacklogProjects(tag: Tag) extends Table[BacklogProjectData](tag, "projects") {
  def id = column[Long]("id", O.PrimaryKey)

  def projectKey = column[String]("projectKey", O.NotNull)

  def * = (id, projectKey) <>(BacklogProjectData.tupled, BacklogProjectData.unapply)
}

object BacklogProjectDao {

  def create(e: BacklogProjectData) = database.withTransaction { implicit session: Session =>
    projects.insert(e)
  }

  def findByProjectKey(projectKey: String): Option[BacklogProjectData] = database.withTransaction { implicit session: Session =>
    projects.filter(_.projectKey === projectKey).firstOption
  }

}

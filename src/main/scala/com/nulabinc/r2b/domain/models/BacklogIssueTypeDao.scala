package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogIssueTypeData(id: Long, projectId: Long, name: String)

class BacklogIssueTypes(tag: Tag) extends Table[BacklogIssueTypeData](tag, "issue_types") {
  def id = column[Long]("id", O.PrimaryKey)

  def projectId = column[Long]("projectId", O.NotNull)

  def name = column[String]("name", O.NotNull)

  def * = (id, projectId, name) <>(BacklogIssueTypeData.tupled, BacklogIssueTypeData.unapply)
}

object BacklogIssueTypeDao {

  def create(e: BacklogIssueTypeData) = database.withTransaction { implicit session: Session =>
    issueTypes.insert(e)
  }

  def createAll(issueTypeDatas: Seq[BacklogIssueTypeData]) = database.withTransaction { implicit session: Session =>
    issueTypeDatas.foreach(create)
  }

  def findByRelationId(projectId: Long, name: String): Option[BacklogIssueTypeData] = database.withTransaction { implicit session: Session =>
    issueTypes.filter(row => (row.name === name) && (row.projectId === projectId)).firstOption
  }

}

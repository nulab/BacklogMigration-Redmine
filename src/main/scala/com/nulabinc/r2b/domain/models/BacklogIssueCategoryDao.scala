package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogIssueCategoryData(id: Long, projectId: Long, name: String)

class BacklogIssueCategories(tag: Tag) extends Table[BacklogIssueCategoryData](tag, "issue_categories") {
  def id = column[Long]("id", O.PrimaryKey)

  def projectId = column[Long]("projectId", O.NotNull)

  def name = column[String]("name", O.NotNull)

  def * = (id, projectId, name) <>(BacklogIssueCategoryData.tupled, BacklogIssueCategoryData.unapply)
}

object BacklogIssueCategoryDao {

  def create(e: BacklogIssueCategoryData) = database.withTransaction { implicit session: Session =>
    issueCategories.insert(e)
  }

  def createAll(categories: Seq[BacklogIssueCategoryData]) = database.withTransaction { implicit session: Session =>
    categories.foreach(create)
  }

  def findByRelationId(projectId: Long, name: String): Option[BacklogIssueCategoryData] = database.withTransaction { implicit session: Session =>
    issueCategories.filter(row => (row.name === name) && (row.projectId === projectId)).firstOption
  }

}
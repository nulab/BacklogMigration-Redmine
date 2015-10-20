package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogCustomFieldData(id: Long, projectId: Long, name: String, typeId: Int)

class BacklogCustomFields(tag: Tag) extends Table[BacklogCustomFieldData](tag, "customFields") {
  def id = column[Long]("id", O.PrimaryKey)

  def projectId = column[Long]("projectId", O.NotNull)

  def name = column[String]("name", O.NotNull)

  def typeId = column[Int]("typeId", O.NotNull)

  def * = (id, projectId, name, typeId) <>(BacklogCustomFieldData.tupled, BacklogCustomFieldData.unapply)
}

object BacklogCustomFieldDao {

  def create(e: BacklogCustomFieldData) = database.withTransaction { implicit session: Session =>
    customFields.insert(e)
  }

  def findByRelationId(projectId: Long, name: String): Option[BacklogCustomFieldData] = database.withTransaction { implicit session: Session =>
    //_.name === name
    customFields.filter(row => (row.name === name) && (row.projectId === projectId)).firstOption
  }

}

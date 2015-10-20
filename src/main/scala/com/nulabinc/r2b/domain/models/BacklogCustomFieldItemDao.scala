package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._
import com.nulabinc.r2b.domain.models.DatabaseCfg._

/**
 * @author uchida
 */
case class BacklogCustomFieldItemData(id: Option[Int], projectId: Long, customFieldId: Long, customFieldItemId: Long, name: String)

class BacklogCustomFieldItems(tag: Tag) extends Table[BacklogCustomFieldItemData](tag, "customFieldItems") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def projectId = column[Long]("projectId", O.NotNull)

  def customFieldId = column[Long]("customFieldId", O.NotNull)

  def customFieldItemId = column[Long]("customFieldItemId", O.NotNull)

  def name = column[String]("name", O.NotNull)

  def * = (id.?, projectId, customFieldId, customFieldItemId, name) <>(BacklogCustomFieldItemData.tupled, BacklogCustomFieldItemData.unapply)
}

object BacklogCustomFieldItemDao {

  def create(e: BacklogCustomFieldItemData) = database.withTransaction { implicit session: Session =>
    if(!isExists(e)) customFieldItems.insert(e)
  }

  def isExists(e: BacklogCustomFieldItemData): Boolean = database.withTransaction { implicit session: Session =>
    customFieldItems.filter(row => (row.projectId === e.projectId)
      && (row.customFieldId === e.customFieldId)
      && (row.customFieldItemId === e.customFieldItemId)
      && (row.name === e.name)).list.nonEmpty
  }

  def findAllByProjectIdAndCustomFieldId(projectId: Long, customFieldId: Long, names: Seq[String]): Seq[BacklogCustomFieldItemData] = database.withTransaction { implicit session: Session =>
    customFieldItems.filter(row => (row.customFieldId === customFieldId) && (row.projectId === projectId) && (row.name inSetBind names)).list
  }

  def findByProjectIdAndCustomFieldId(projectId: Long, customFieldId: Long, name: String): Option[BacklogCustomFieldItemData] = database.withTransaction { implicit session: Session =>
    customFieldItems.filter(row => (row.customFieldId === customFieldId) && (row.projectId === projectId) && (row.name === name)).firstOption
  }

}

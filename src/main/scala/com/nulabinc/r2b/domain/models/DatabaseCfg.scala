package com.nulabinc.r2b.domain.models

import scala.slick.driver.H2Driver.simple._

/**
 * @author uchida
 */
object DatabaseCfg {
  val database = Database.forConfig("r2bdb")

  lazy val users = new TableQuery(tag => new BacklogUsers(tag))
  lazy val projects = new TableQuery(tag => new BacklogProjects(tag))
  lazy val customFields = new TableQuery(tag => new BacklogCustomFields(tag))
  lazy val customFieldItems = new TableQuery(tag => new BacklogCustomFieldItems(tag))
  lazy val issueTypes = new TableQuery(tag => new BacklogIssueTypes(tag))
  lazy val issueCategories = new TableQuery(tag => new BacklogIssueCategories(tag))

  def init() = {
    database.withTransaction { implicit session =>
      users.ddl.create
      projects.ddl.create
      customFields.ddl.create
      customFieldItems.ddl.create
      issueTypes.ddl.create
      issueCategories.ddl.create
    }
  }

}

package com.nulabinc.r2b.core

import com.nulabinc.backlog.importer.conf.ImportConfig
import com.nulabinc.backlog.importer.controllers.ImportController
import com.nulabinc.backlog.migration.conf.BacklogDirectory
import com.nulabinc.r2b.conf.{AppConfiguration, RedmineDirectory}
import com.nulabinc.r2b.controllers.{ConvertController, ExportController}
import org.scalatest.BeforeAndAfterAll

import scalax.file.Path

/**
  * @author uchida
  */
class MigrationSpec extends R2BSpec with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    Path.fromString(RedmineDirectory.ROOT).deleteRecursively(force = true, continueOnFailure = true)
    Path.fromString(BacklogDirectory.ROOT).deleteRecursively(force = true, continueOnFailure = true)

    ExportController.execute(config, propertyMappingFiles.user.getNeedUsers())
    ConvertController.execute(config, propertyMappingFiles.user.getNeedUsers())
    ImportController.execute(getImportConfig(config))
  }

  override def afterAll(): Unit = {
    backlog.deleteProject(config.projectKeyMap.getBacklogKey())
  }

  private[this] def getImportConfig(config: AppConfiguration) =
    ImportConfig(
      url = config.backlogConfig.url,
      key = config.backlogConfig.key,
      projectKey = config.projectKeyMap.getBacklogKey())

}

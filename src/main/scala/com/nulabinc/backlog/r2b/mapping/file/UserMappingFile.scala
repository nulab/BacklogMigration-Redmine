package com.nulabinc.backlog.r2b.mapping.file

import java.nio.file.Path

import better.files.File
import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration, MappingDirectory}
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{UserService => BacklogUserService}
import com.nulabinc.backlog.migration.common.utils.{IOUtil, Logging, StringUtil}
import com.nulabinc.backlog.r2b.mapping.domain.MappingJsonProtocol._
import com.nulabinc.backlog.r2b.mapping.domain.{Mapping, MappingsWrapper}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{UserService => RedmineUserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}
import spray.json.JsonParser

/**
  * @author uchida
  */
class UserMappingFile(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration, users: Seq[RedmineUser])
    extends Logging
    with BacklogConfiguration {

  private[this] val redmineItems = getRedmineItems()
  private[this] val backlogItems = getBacklogItems()

  private[this] def getRedmineItems(): Seq[MappingItem] = {
    val injector    = RedmineInjector.createInjector(redmineApiConfig)
    val userService = injector.getInstance(classOf[RedmineUserService])

    def resolve(user: RedmineUser): Option[RedmineUser] = {
      (Option(user.getLogin), Option(user.getFullName)) match {
        case (Some(_), Some(_)) => Some(user)
        case _                  => userService.optUserOfId(user.getId)
      }
    }

    def condition(user: RedmineUser): Boolean = {
      StringUtil.notEmpty(user.getLogin).nonEmpty
    }

    def createItem(user: RedmineUser): MappingItem = {
      MappingItem(user.getLogin, user.getFullName)
    }

    val redmineUsers = users.flatMap(resolve).filter(condition)
    redmineUsers.map(createItem)
  }

  private[this] def getBacklogItems(): Seq[MappingItem] = {
    def createItem(user: BacklogUser): MappingItem = {
      if (backlogApiConfig.url.contains(NaiSpaceDomain)) {
        MappingItem(user.optMailAddress.getOrElse(""), user.name)
      } else {
        MappingItem(user.optUserId.getOrElse(""), user.name)
      }
    }
    val backlogUsers = allUsers()
    backlogUsers.map(createItem)
  }

  private[this] def allUsers(): Seq[BacklogUser] = {
    val injector    = BacklogInjector.createInjector(backlogApiConfig)
    val userService = injector.getInstance(classOf[BacklogUserService])
    userService.allUsers()
  }

  private[this] def convertForNAI(backlogUsers: Seq[BacklogUser])(mapping: Mapping) = {
    if (backlogApiConfig.url.contains(NaiSpaceDomain)) {
      val targetBacklogUser = backlogUsers
        .find(backlogUser => backlogUser.optMailAddress.getOrElse("") == mapping.backlog)
        .getOrElse(throw new NoSuchElementException(s"User ${mapping.backlog} not found"))
      mapping.copy(backlog = targetBacklogUser.optUserId.getOrElse(s"UserId ${mapping.backlog} not found"))
    } else mapping
  }

  def tryUnmarshal(): Seq[Mapping] = {
    val path    = File(filePath).path.toAbsolutePath
    val json    = IOUtil.input(path).getOrElse("")
    val convert = convertForNAI(allUsers()) _
    JsonParser(json).convertTo[MappingsWrapper].mappings.map(convert)
  }

  def matchItem(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name).getOrElse("")

  def redmines: Seq[MappingItem] = redmineItems

  def backlogs: Seq[MappingItem] = backlogItems

  def filePath: Path = MappingDirectory.default.userMappingFilePath

  def itemName: String = Messages("common.users")

  def description: String = {
    Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))
  }

  def isDisplayDetail: Boolean = true

}

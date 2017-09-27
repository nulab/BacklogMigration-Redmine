package com.nulabinc.backlog.r2b.mapping.file

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration}
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{UserService => BacklogUserService}
import com.nulabinc.backlog.migration.common.utils.StringUtil
import com.nulabinc.backlog.r2b.mapping.core.MappingDirectory
import com.nulabinc.backlog.r2b.mapping.domain.MappingJsonProtocol._
import com.nulabinc.backlog.r2b.mapping.domain.{Mapping, MappingsWrapper}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{UserService => RedmineUserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}
import spray.json.JsonParser

import scalax.file.Path

/**
  * @author uchida
  */
class UserMappingFile(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration, users: Seq[RedmineUser])
    extends MappingFile
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

  override def tryUnmarshal(): Seq[Mapping] = {
    val path    = Path.fromString(filePath)
    val json    = path.lines().mkString
    val convert = convertForNAI(allUsers()) _
    JsonParser(json).convertTo[MappingsWrapper].mappings.map(convert)
  }

  override def matchItem(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name).getOrElse("")

  override def redmines: Seq[MappingItem] = redmineItems

  override def backlogs: Seq[MappingItem] = backlogItems

  override def filePath: String = MappingDirectory.USER_MAPPING_FILE

  override def itemName: String = Messages("common.users")

  override def description: String = {
    Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))
  }

  override def isDisplayDetail: Boolean = true

}

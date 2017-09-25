package com.nulabinc.backlog.r2b.mapping.core

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{UserService => BacklogUserService}
import com.nulabinc.backlog.migration.common.utils.StringUtil
import com.nulabinc.backlog.r2b.mapping.domain.MappingItem
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{UserService => RedmineUserService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}

/**
  * @author uchida
  */
class UserMappingFile(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration, users: Seq[RedmineUser])
    extends MappingFile {

  private[this] val redmineDatas = loadRedmine()
  private[this] val backlogDatas = loadBacklog()

  private[this] def loadRedmine(): Seq[MappingItem] = {
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

    val redmineUsers = users.toSeq.flatMap(resolve).filter(condition)
    redmineUsers.map(createItem)
  }

  private[this] def loadBacklog(): Seq[MappingItem] = {
    def createItem(user: BacklogUser): MappingItem = {
      MappingItem(user.optUserId.getOrElse(""), user.name)
    }

    val injector     = BacklogInjector.createInjector(backlogApiConfig)
    val userService  = injector.getInstance(classOf[BacklogUserService])
    val backlogUsers = userService.allUsers()
    backlogUsers.map(createItem)
  }

  override def findMatchItem(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name).getOrElse("")

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = MappingDirectory.USER_MAPPING_FILE

  override def itemName: String = Messages("common.users")

  override def description: String = {
    Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))
  }

  override def isDisplayDetail: Boolean = true

}

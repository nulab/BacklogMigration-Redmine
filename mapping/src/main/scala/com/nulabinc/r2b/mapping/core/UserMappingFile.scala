package com.nulabinc.r2b.mapping.core

import com.nulabinc.backlog.migration.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.nulabinc.backlog.migration.modules.ServiceInjector
import com.nulabinc.backlog.migration.service.UserService
import com.nulabinc.r2b.mapping.domain.MappingItem
import com.nulabinc.r2b.redmine.conf.RedmineConfig
import com.nulabinc.r2b.redmine.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}

/**
  * @author uchida
  */
class UserMappingFile(redmineApiConfig: RedmineConfig, backlogApiConfig: BacklogApiConfiguration, mappingData: MappingData) extends MappingFile {

  private[this] val backlogDatas = loadBacklog()
  private[this] val redmineDatas = loadRedmine()

  def getNeedUsers(): Seq[RedmineUser] = mappingData.users.toSeq

  private[this] def loadRedmine(): Seq[MappingItem] = {
    val redmineService: RedmineService = new RedmineService(redmineApiConfig)

    val redmineUsers: Seq[RedmineUser] = mappingData.users.toSeq
      .flatMap(user => {
        if (Option(user.getLogin).isDefined && Option(user.getFullName).isDefined) Some(user)
        else redmineService.getUserById(user.getId)
      })
      .filter(user => user.getLogin != "")

    val redmines: Seq[MappingItem] = redmineUsers.map(redmineUser => MappingItem(redmineUser.getLogin, redmineUser.getFullName))
    redmines
  }

  private[this] def loadBacklog(): Seq[MappingItem] = {
    val injector                       = ServiceInjector.createInjector(backlogApiConfig)
    val userService                    = injector.getInstance(classOf[UserService])
    val backlogUsers: Seq[BacklogUser] = userService.allUsers()
    val backlogs: Seq[MappingItem]     = backlogUsers.map(backlogUser => MappingItem(backlogUser.optUserId.getOrElse(""), backlogUser.name))
    backlogs
  }

  override def matchWithBacklog(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name).getOrElse("")

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = MappingDirectory.USER_MAPPING_FILE

  override def itemName: String = Messages("common.users")

  override def description: String = {
    val description: String =
      Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))
    description
  }

  override def isDisplayDetail: Boolean = true

}

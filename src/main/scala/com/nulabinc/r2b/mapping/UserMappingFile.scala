package com.nulabinc.r2b.mapping

import com.nulabinc.backlog4j.{User => BacklogUser}
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.service.{BacklogService, RedmineService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}

/**
  * @author uchida
  */
class UserMappingFile(config: AppConfiguration, mappingData: MappingData) extends MappingFile {

  private[this] val backlogDatas = loadBacklog()
  private[this] val redmineDatas = loadRedmine()

  def getNeedUsers(): Seq[RedmineUser] = mappingData.users.toSeq

  private[this] def loadRedmine(): Seq[MappingItem] = {
    val redmineService: RedmineService = new RedmineService(config.redmineConfig)

    val redmineUsers: Seq[RedmineUser] = mappingData.users.toSeq.flatMap(user => {
      if (Option(user.getLogin).isDefined && Option(user.getFullName).isDefined) Some(user)
      else redmineService.getUserById(user.getId)
    }).filter(user => user.getLogin != "")

    val redmines: Seq[MappingItem] = redmineUsers.map(redmineUser => MappingItem(redmineUser.getLogin, redmineUser.getFullName))
    redmines
  }

  private[this] def loadBacklog(): Seq[MappingItem] = {
    val backlogService: BacklogService = new BacklogService(config.backlogConfig)
    val backlogUsers: Seq[BacklogUser] = backlogService.users
    val backlogs: Seq[MappingItem] = backlogUsers.map(backlogUser => MappingItem(backlogUser.getUserId, backlogUser.getName))
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
      Messages("mapping.possible_values", itemName, backlogs.map(_.name).mkString(","))
    description
  }

  override def isDisplayDetail: Boolean = true

}

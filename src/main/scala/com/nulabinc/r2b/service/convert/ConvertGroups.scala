package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.importer.domain.{BacklogGroup, BacklogGroupsWrapper}
import com.nulabinc.r2b.domain.{RedmineGroup, RedmineUser}
import com.nulabinc.r2b.service.ConvertUserMapping

/**
  * @author uchida
  */
class ConvertGroups {

  val userMapping: ConvertUserMapping = new ConvertUserMapping()

  def execute(redmineGroups: Seq[RedmineGroup], redmineUsers: Seq[RedmineUser]): BacklogGroupsWrapper = {
    val backlogGroups: Seq[BacklogGroup] = redmineGroups.map(redmineGroup => getBacklogGroup(redmineGroup, redmineUsers))
    BacklogGroupsWrapper(backlogGroups)
  }

  private def getBacklogGroup(redmineGroup: RedmineGroup, redmineUsers: Seq[RedmineUser]): BacklogGroup = {
    val groupUserIds: Seq[String] = getUserIdsByGroupId(redmineGroup.name, redmineUsers)
    BacklogGroup(redmineGroup.name, groupUserIds.map(userMapping.convert))
  }

  private def getUserIdsByGroupId(groupName: String, redmineUsers: Seq[RedmineUser]): Seq[String] =
    redmineUsers.filter(redmineUser => redmineUser.groups.contains(groupName)).map(_.login)

}
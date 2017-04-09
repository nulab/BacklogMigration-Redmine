package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.converter.Writes
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.nulabinc.r2b.mapping.core.ConvertUserMapping
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
class UserWrites @Inject()() extends Writes[User, BacklogUser] {

  val userMapping = new ConvertUserMapping()

  override def writes(user: User): BacklogUser = {
    BacklogUser(optId = Option(user.getId.intValue()),
                optUserId = Option(user.getLogin).map(userMapping.convert),
                optPassword = Option(user.getPassword),
                name = user.getFullName,
                optMailAddress = Option(user.getMail),
                roleType = BacklogConstantValue.USER_ROLE)
  }

}

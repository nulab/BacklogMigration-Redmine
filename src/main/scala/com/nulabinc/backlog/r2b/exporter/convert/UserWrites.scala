package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.mapping.service.MappingUserService
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
private[exporter] class UserWrites @Inject() (propertyValue: PropertyValue, mappingUserService: MappingUserService)
    extends Writes[User, BacklogUser]
    with Logging {

  override def writes(user: User): BacklogUser = {
    (Option(user.getLogin), Option(user.getFullName)) match {
      case (Some(_), Some(_)) => toBacklog(user)
      case _ =>
        propertyValue.optUserOfId(user.getId) match {
          case Some(user) => toBacklog(user)
          case None       => toBacklog(Option(user.getFirstName).getOrElse(Messages("common.anonymous")))
        }
    }
  }

  private[this] def toBacklog(user: User): BacklogUser = {
    BacklogUser(
      optId = Option(user.getId.intValue()),
      optUserId = Option(user.getLogin).map(mappingUserService.convert),
      optPassword = Option(user.getPassword),
      name = user.getFullName,
      optMailAddress = Option(user.getMail),
      roleType = BacklogConstantValue.USER_ROLE
    )
  }

  private[this] def toBacklog(name: String): BacklogUser = {
    BacklogUser(optId = None, optUserId = None, optPassword = None, name = name, optMailAddress = None, roleType = BacklogConstantValue.USER_ROLE)
  }

}

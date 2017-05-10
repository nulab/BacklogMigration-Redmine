package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{BacklogGroup, BacklogUser}
import com.nulabinc.r2b.redmine.domain.PropertyValue
import com.taskadapter.redmineapi.bean.{Group, Membership, User}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class GroupsWrites @Inject()(implicit val userWrites: UserWrites, propertyValue: PropertyValue) extends Writes[Seq[Membership], Seq[BacklogGroup]] {

  override def writes(memberships: Seq[Membership]): Seq[BacklogGroup] = {
    val users = propertyValue.users
    memberships.filter(condition).map(_.getGroup).map(toBacklog).map(_(users))
  }

  private[this] def toBacklog(group: Group)(users: Seq[User]): BacklogGroup = {
    BacklogGroup(name = group.getName, members = toBacklog(group.getName, users))
  }

  private[this] def toBacklog(name: String, users: Seq[User]): Seq[BacklogUser] = {
    users.filter(user => condition(name, user)).map(Convert.toBacklog(_))
  }

  def condition(membership: Membership) = Option(membership.getGroup).isDefined

  def condition(name: String, user: User) = user.getGroups.asScala.toSeq.map(_.getName).contains(name)

}

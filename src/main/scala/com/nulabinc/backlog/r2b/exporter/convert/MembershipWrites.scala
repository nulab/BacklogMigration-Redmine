package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.taskadapter.redmineapi.bean.Membership

/**
 * @author uchida
 */
private[exporter] class MembershipWrites @Inject() (implicit val userWrites: UserWrites)
    extends Writes[Seq[Membership], Seq[BacklogUser]] {

  override def writes(memberships: Seq[Membership]): Seq[BacklogUser] = {
    memberships.filter(condition).map(_.getUser).map(Convert.toBacklog(_))
  }

  private[this] def condition(membership: Membership) = Option(membership.getUser).isDefined

}

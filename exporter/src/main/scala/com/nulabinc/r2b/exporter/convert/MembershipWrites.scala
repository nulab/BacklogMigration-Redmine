package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.converter.{Convert, Writes}
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.taskadapter.redmineapi.bean.Membership

/**
  * @author uchida
  */
class MembershipWrites @Inject()(implicit val userWrites: UserWrites) extends Writes[Seq[Membership], Seq[BacklogUser]] {

  override def writes(memberships: Seq[Membership]): Seq[BacklogUser] = {
    memberships.filter(condition).map(_.getUser).map(Convert.toBacklog(_))
  }

  private[this] def condition(membership: Membership) = Option(membership.getUser).isDefined

}

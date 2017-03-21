package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.Membership

/**
  * @author uchida
  */
trait MembershipService {

  def allMemberships(): Seq[Membership]

}

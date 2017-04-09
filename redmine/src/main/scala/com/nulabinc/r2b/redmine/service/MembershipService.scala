package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.Membership

/**
  * @author uchida
  */
trait MembershipService {

  def allMemberships(): Seq[Membership]

}

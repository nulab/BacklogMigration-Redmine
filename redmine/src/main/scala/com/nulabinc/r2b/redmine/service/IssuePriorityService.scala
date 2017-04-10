package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.IssuePriority

/**
  * @author uchida
  */
trait IssuePriorityService {

  def allIssuePriorities(): Seq[IssuePriority]

}

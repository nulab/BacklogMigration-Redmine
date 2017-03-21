package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.IssuePriority

/**
  * @author uchida
  */
trait IssuePriorityService {

  def allIssuePriorities(): Seq[IssuePriority]

}

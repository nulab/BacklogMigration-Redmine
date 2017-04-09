package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.IssueStatus

/**
  * @author uchida
  */
trait StatusService {

  def allStatuses(): Seq[IssueStatus]

}

package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.IssueStatus

/**
  * @author uchida
  */
trait StatusService {

  def allStatuses(): Seq[IssueStatus]

}

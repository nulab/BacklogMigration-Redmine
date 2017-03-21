package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.migration.domain.BacklogIssue
import com.nulabinc.r2b.domain.RedmineIssue

/**
  * @author uchida
  */
trait ConvertIssueService {

  def convert(redmineIssue: RedmineIssue): BacklogIssue

}

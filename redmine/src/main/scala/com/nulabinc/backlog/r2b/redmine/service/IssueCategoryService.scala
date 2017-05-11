package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.bean.IssueCategory

/**
  * @author uchida
  */
trait IssueCategoryService {

  def allCategories(): Seq[IssueCategory]

}

package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.IssueCategory

/**
  * @author uchida
  */
trait IssueCategoryService {

  def allCategories(): Seq[IssueCategory]

}

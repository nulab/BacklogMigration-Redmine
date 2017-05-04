package com.nulabinc.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogIssueCategory
import com.taskadapter.redmineapi.bean.IssueCategory

/**
  * @author uchida
  */
class IssueCategoriesWrites @Inject()() extends Writes[Seq[IssueCategory], Seq[BacklogIssueCategory]] {

  override def writes(categories: Seq[IssueCategory]): Seq[BacklogIssueCategory] = {
    categories.map(toBacklog)
  }

  private[this] def toBacklog(category: IssueCategory) = {
    BacklogIssueCategory(optId = Some(category.getId.intValue()), name = category.getName, delete = false)
  }

}

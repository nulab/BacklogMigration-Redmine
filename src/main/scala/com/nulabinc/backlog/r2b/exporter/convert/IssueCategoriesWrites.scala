package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogIssueCategory
import com.taskadapter.redmineapi.bean.IssueCategory

/**
  * @author uchida
  */
private[exporter] class IssueCategoriesWrites @Inject() ()
    extends Writes[Seq[IssueCategory], Seq[BacklogIssueCategory]] {

  override def writes(categories: Seq[IssueCategory]): Seq[BacklogIssueCategory] = {
    categories.map(toBacklog)
  }

  private[this] def toBacklog(category: IssueCategory) = {
    BacklogIssueCategory(
      optId = Some(category.getId.intValue()),
      name = category.getName,
      delete = false
    )
  }

}

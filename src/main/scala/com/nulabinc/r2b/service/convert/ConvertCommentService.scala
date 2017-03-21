package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.migration.domain.BacklogComment
import com.nulabinc.r2b.domain.RedmineJournal

/**
  * @author uchida
  */
trait ConvertCommentService {

  def convert(journals: Seq[RedmineJournal], issueId: Int): Seq[BacklogComment]

}

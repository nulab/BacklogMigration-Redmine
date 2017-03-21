package com.nulabinc.r2b.service.convert

import com.nulabinc.r2b.domain.RedmineJournalDetail

/**
  * @author uchida
  */
trait ConvertJournalDetailService {

  def needNote(detail: RedmineJournalDetail, issueId: Int): Boolean

  def getValue(detail: RedmineJournalDetail, issueId: Int): String

}

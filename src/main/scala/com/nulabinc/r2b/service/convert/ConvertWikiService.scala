package com.nulabinc.r2b.service.convert

import com.nulabinc.backlog.migration.domain.BacklogWiki
import com.nulabinc.r2b.domain.RedmineWikiPage

/**
  * @author uchida
  */
trait ConvertWikiService {

  def convert(redmineWikiPage: RedmineWikiPage): BacklogWiki

}

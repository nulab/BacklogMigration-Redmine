package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.{Issue, WikiPageDetail}

/**
  * @author uchida
  */
trait AttachmentDownloadService {

  def issue(apiKey: String, issue: Issue)

  def wiki(apiKey: String, wikiPageDetail: WikiPageDetail)

}

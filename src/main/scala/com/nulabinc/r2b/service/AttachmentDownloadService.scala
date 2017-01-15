package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.{Issue, WikiPageDetail}

/**
  * @author uchida
  */
trait AttachmentDownloadService {

  def issue(apiKey: String, projectIdentifier: String, issue: Issue)

  def wiki(apiKey: String, projectIdentifier: String, wikiPageDetail: WikiPageDetail)

}

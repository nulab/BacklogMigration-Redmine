package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.{WikiPage, WikiPageDetail}

/**
  * @author uchida
  */
trait WikiService {

  def allWikis(): Seq[WikiPage]

  def wikiDetail(pageTitle: String): WikiPageDetail

}

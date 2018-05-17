package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.bean.{WikiPage, WikiPageDetail}

/**
  * @author uchida
  */
trait WikiService {

  def allWikis(): Seq[WikiPage]

  def optWikiDetail(pageTitle: String): Option[WikiPageDetail]

}

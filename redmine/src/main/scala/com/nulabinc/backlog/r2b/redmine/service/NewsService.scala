package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.bean.News

/**
  * @author uchida
  */
trait NewsService {

  def allNews(): Seq[News]

}

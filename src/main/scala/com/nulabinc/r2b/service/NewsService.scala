package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.News

/**
  * @author uchida
  */
trait NewsService {

  def allNews(): Seq[News]

}

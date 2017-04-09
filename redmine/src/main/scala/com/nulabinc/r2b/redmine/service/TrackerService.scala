package com.nulabinc.r2b.redmine.service

import com.taskadapter.redmineapi.bean.Tracker

/**
  * @author uchida
  */
trait TrackerService {

  def allTrackers(): Seq[Tracker]

}

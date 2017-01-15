package com.nulabinc.r2b.service

import com.taskadapter.redmineapi.bean.Tracker

/**
  * @author uchida
  */
trait TrackerService {

  def allTrackers(): Seq[Tracker]

}

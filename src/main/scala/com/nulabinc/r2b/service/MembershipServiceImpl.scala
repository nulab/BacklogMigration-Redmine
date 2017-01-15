package com.nulabinc.r2b.service

import javax.inject.{Inject, Named}

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Membership

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class MembershipServiceImpl @Inject()(@Named("projectKey") projectKey: String, redmine: RedmineManager) extends MembershipService with Logging {

  override def allMemberships(): Seq[Membership] =
    try {
      redmine.getMembershipManager.getMemberships(projectKey).asScala
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
        Seq.empty[Membership]
    }

}

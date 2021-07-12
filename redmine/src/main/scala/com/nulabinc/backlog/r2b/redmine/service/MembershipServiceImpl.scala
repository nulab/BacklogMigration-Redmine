package com.nulabinc.backlog.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.Membership

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class MembershipServiceImpl @Inject() (
    apiConfig: RedmineApiConfiguration,
    redmine: RedmineManager
) extends MembershipService
    with Logging {

  override def allMemberships(): Seq[Membership] =
    try {
      redmine.getMembershipManager
        .getMemberships(apiConfig.projectKey)
        .asScala
        .toSeq
    } catch {
      case e: Throwable =>
        logger.warn(e.getMessage, e)
        Seq.empty[Membership]
    }

}

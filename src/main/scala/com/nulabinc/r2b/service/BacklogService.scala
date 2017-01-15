package com.nulabinc.r2b.service

import com.nulabinc.backlog.migration.conf.BacklogConfig
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class BacklogService(conf: BacklogConfig) extends Logging {

  val backlog: BacklogClient = getBacklogClient

  def users: Seq[User] = backlog.getUsers.asScala

  def getStatuses: Seq[Status] = backlog.getStatuses.asScala

  def getPriorities: Seq[Priority] = backlog.getPriorities.asScala

  def optProject(projectKey: String): Option[Project] = try {
    Some(backlog.getProject(projectKey))
  } catch {
    case e: BacklogAPIException =>
      if (!(e.getMessage.contains("No project") || e.getMessage.contains("No such project"))) log.error(e)
      None
  }

  private def getBacklogClient: BacklogClient = {
    val url = conf.url
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(url)
    val configure: BacklogConfigure = backlogPackageConfigure.apiKey(conf.key)
    new BacklogClientFactory(configure).newClient()
  }

}

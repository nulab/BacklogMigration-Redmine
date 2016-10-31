package com.nulabinc.r2b.service

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.r2b.actor.utils.R2BLogging

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class BacklogService(conf: BacklogConfig) extends R2BLogging {

  val backlog: BacklogClient = getBacklogClient

  def getUsers: Seq[User] = backlog.getUsers.asScala

  def getStatuses: Seq[Status] = backlog.getStatuses.asScala

  def getPriorities: Seq[Priority] = backlog.getPriorities.asScala

  def getIssues(projectKey: String): Seq[Issue] = {
    val optProject: Option[Project] = getProject(projectKey)
    if (optProject.isDefined) {
      val params: GetIssuesParams = new GetIssuesParams(List(optProject.get.getId.asInstanceOf[java.lang.Long]).asJava)
      backlog.getIssues(params).asScala
    } else Seq.empty[Issue]
  }


  def getProject(projectKey: String): Option[Project] = try {
    Some(backlog.getProject(projectKey))
  } catch {
    case e: BacklogAPIException =>
      if (!(e.getMessage.contains("No project") || e.getMessage.contains("No such project"))) error(e)
      None
  }

  private def getBacklogClient: BacklogClient = {
    val url = conf.url
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(url)
    val configure: BacklogConfigure = backlogPackageConfigure.apiKey(conf.key)
    new BacklogClientFactory(configure).newClient()
  }

}

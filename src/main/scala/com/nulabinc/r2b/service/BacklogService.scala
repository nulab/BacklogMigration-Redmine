package com.nulabinc.r2b.service

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j._
import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import scala.collection.JavaConverters._

/**
 * @author uchida
 */
class BacklogService(conf: BacklogConfig) {

  val backlog: BacklogClient = getBacklogClient

  def getUsers: Seq[User] = backlog.getUsers.asScala

  def getStatuses: Seq[Status] = backlog.getStatuses.asScala

  def getPriorities: Seq[Priority] = backlog.getPriorities.asScala

  def getIssues(projectKey: String): Seq[Issue] = {
    val result: Either[Throwable, Project] = getProject(projectKey)
    if (result.isLeft) return Seq.empty[Issue]

    val params: GetIssuesParams = new GetIssuesParams(List(result.right.get.getId.asInstanceOf[java.lang.Long]).asJava)
    backlog.getIssues(params).asScala
  }


  def getProject(projectKey: String): Either[Throwable, Project] = try {
    Right(backlog.getProject(projectKey))
  } catch {
    case e: BacklogAPIException => Left(e)
  }

  private def getBacklogClient: BacklogClient = {
    val url = conf.url
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(url)
    val configure: BacklogConfigure = backlogPackageConfigure.apiKey(conf.key)
    new BacklogClientFactory(configure).newClient()
  }

}

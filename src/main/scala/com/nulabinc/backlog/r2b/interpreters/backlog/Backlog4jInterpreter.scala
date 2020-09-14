package com.nulabinc.backlog.r2b.interpreters.backlog

import cats.~>
import com.nulabinc.backlog.r2b.dsl.BacklogDSL.{BacklogProgram, BacklogResponse}
import com.nulabinc.backlog.r2b.dsl._
import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.backlog4j.conf.BacklogPackageConfigure
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory, Issue}
import monix.eval.Task

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class Backlog4jInterpreter(url: String, key: String)(implicit val exc: ExecutionContext) extends (BacklogADT ~> Task) {

  private val backlogPackageConfigure = new BacklogPackageConfigure(url)
  private val configure               = backlogPackageConfigure.apiKey(key)
  private val client: BacklogClient   = new BacklogClientFactory(configure).newClient()

  def run[A](program: BacklogProgram[A]): Task[A] =
    program.foldMap(this)

  def getProjectIssues(projectId: Long, offset: Int, count: Int): Seq[Issue] = {
    val params: GetIssuesParams = new GetIssuesParams(List(projectId).asJava)
    params.offset(offset.toLong)
    params.count(count)
    params.sort(GetIssuesParams.SortKey.Created)
    params.order(GetIssuesParams.Order.Asc)
    client.getIssues(params).asScala.toSeq
  }

  def deleteIssue(issue: Issue): Task[BacklogResponse[Unit]] = Task {
    try {
      Right(client.deleteIssue(issue.getId))
    } catch {
      case ex: Throwable => Left(ResponseError(ex))
    }
  }

  override def apply[A](fa: BacklogADT[A]): Task[A] = fa match {
    case Pure(a) => Task(a)
    case GetProject(projectKey) =>
      runRequest()(client.getProject(projectKey))
    case GetProjectIssues(projectId, offset, count) =>
      runRequest()(getProjectIssues(projectId, offset, count))
    case DeleteIssue(issue) => deleteIssue(issue)
  }

  private def runRequest[A]()(f: A): Task[BacklogResponse[A]] = Task {
    try {
      Right(f)
    } catch {
      case ex: Throwable => Left(ResponseError(ex))
    }
  }

}

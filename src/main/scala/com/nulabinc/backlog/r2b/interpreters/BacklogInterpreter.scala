package com.nulabinc.backlog.r2b.interpreters

import cats.free.Free
import cats.~>
import com.nulabinc.backlog.r2b.interpreters.BacklogDSL.{BacklogProgram, BacklogResponse, IssueStreamF}
import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory, Issue, Project}
import com.nulabinc.backlog4j.conf.BacklogPackageConfigure
import monix.eval.Task

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._
import scala.collection.mutable

sealed trait BacklogError
case class ResponseError(ex: Throwable) extends BacklogError

sealed trait BacklogADT[A]
case class GetProject(projectKey: String) extends BacklogADT[BacklogResponse[Project]]
case class GetProjectIssues(projectKey: String, offset: Int, count: Int) extends BacklogADT[BacklogResponse[Seq[Issue]]]

object BacklogDSL {

  type BacklogProgram[A] = Free[BacklogADT, A]
  type IssueStreamF[A] = (Seq[Issue], Int, Int) => BacklogProgram[A]
  type BacklogResponse[A] = Either[BacklogError, A]

  def getProject(projectKey: String): BacklogProgram[BacklogResponse[Project]] =
    Free.liftF(GetProject(projectKey))

  def getProjectIssues(projectKey: String, offset: Int, count: Int): BacklogProgram[BacklogResponse[Seq[Issue]]] =
    Free.liftF(GetProjectIssues(projectKey, offset, count))

}


class BacklogInterpreter(url: String, key: String)
                        (implicit val exc: ExecutionContext) extends (BacklogADT ~> Task) {

  private val backlogPackageConfigure = new BacklogPackageConfigure(url)
  private val configure = backlogPackageConfigure.apiKey(key)
  private val client: BacklogClient = new BacklogClientFactory(configure).newClient()

  def run[A](program: BacklogProgram[A]): Task[A] =
    program.foldMap(this)

  def getProjectIssues(projectKey: String, offset: Int, count: Int): Seq[Issue] = {
    val params: GetIssuesParams = new GetIssuesParams(List(projectKey).asJava)
    params.offset(offset.toLong)
    params.count(count)
    params.sort(GetIssuesParams.SortKey.Created)
    params.order(GetIssuesParams.Order.Asc)
    client.getIssues(params).asScala
  }

  def streamIssue[A](projectKey: String, offset: Int, limit: Int)(f: IssueStreamF[A]): BacklogProgram[A] = {
    // @tailrec Q5. tailrec is fail
    def go(current: Int): BacklogProgram[A] = {
      val params: GetIssuesParams = new GetIssuesParams(List(projectKey).asJava)
      params.offset(offset.toLong)
      params.count(limit)
      params.sort(GetIssuesParams.SortKey.Created)
      params.order(GetIssuesParams.Order.Asc)

      runRequest(() => client.getIssues(params).asScala) match {
        case Right(issues) =>
      }orFail.flatMap { users =>
        if (users.isEmpty)
          f(users, current, limit)
        else
          f(users, current, limit).flatMap(_ => go(current + limit))
      }
    }
    go(0)
  }

  override def apply[A](fa: BacklogADT[A]): Task[A] = fa match {
    case GetProject(projectKey) =>
      runRequest(() => client.getProject(projectKey))
    case GetProjectIssues(projectKey, offset, count) =>
      runRequest(() => getProjectIssues(projectKey, offset, count))

  }

  private def runRequest[A](f: () => A): Task[BacklogResponse[A]] = Task {
    try {
      Right(f())
    } catch {
      case ex: Throwable => Left(ResponseError(ex))
    }
  }

}

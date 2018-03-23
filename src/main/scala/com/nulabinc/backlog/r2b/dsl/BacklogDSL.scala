package com.nulabinc.backlog.r2b.dsl

import cats.free.Free
import com.nulabinc.backlog4j.{Issue, Project}

sealed trait BacklogError
case class ResponseError(ex: Throwable) extends BacklogError

object BacklogDSL {

  type BacklogProgram[A] = Free[BacklogADT, A]
  type IssueStreamF[A] = (Seq[Issue], Int, Int) => BacklogProgram[A]
  type BacklogResponse[A] = Either[BacklogError, A]

  def pure[A](a: A): BacklogProgram[A] =
    Free.liftF(Pure(a))

  def getProject(projectKey: String): BacklogProgram[BacklogResponse[Project]] =
    Free.liftF(GetProject(projectKey))

  def getProjectIssues(projectKey: String, offset: Int, count: Int): BacklogProgram[BacklogResponse[Seq[Issue]]] =
    Free.liftF(GetProjectIssues(projectKey, offset, count))

  def deleteIssue(issue: Issue): BacklogProgram[BacklogResponse[Unit]] =
    Free.liftF(DeleteIssue(issue))

}


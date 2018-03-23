package com.nulabinc.backlog.r2b.dsl

import com.nulabinc.backlog.r2b.dsl.BacklogDSL.BacklogResponse
import com.nulabinc.backlog4j.{Issue, Project}

sealed trait BacklogADT[A]
case class Pure[A](a: A) extends BacklogADT[A]
case class GetProject(projectKey: String) extends BacklogADT[BacklogResponse[Project]]
case class GetProjectIssues(projectKey: String, offset: Int, count: Int) extends BacklogADT[BacklogResponse[Seq[Issue]]]
case class DeleteIssue(issue: Issue) extends BacklogADT[BacklogResponse[Unit]]

package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.Issue

/**
  * @author uchida
  */
trait IssueService {

  def countIssues(): Int

  def allIssues(params: Map[String, String]): Seq[Issue]

  def issueOfId(id: Integer, include: Include*): Issue

  def tryIssueOfId(id: Integer, include: Include*): Either[Throwable, Issue]

}

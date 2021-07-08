package com.nulabinc.backlog.r2b.mapping.collector.core

import com.nulabinc.backlog.r2b.redmine.domain.RedmineProjectId
import com.nulabinc.backlog.r2b.redmine.service.{
  IssueService,
  UserService,
  WikiService
}

case class MappingContext(
    projectId: RedmineProjectId,
    issueService: IssueService,
    wikiService: WikiService,
    userService: UserService
)

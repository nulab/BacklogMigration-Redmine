package com.nulabinc.backlog.r2b.mapping.collector.core

import javax.inject.Inject

import com.nulabinc.backlog.r2b.redmine.domain.RedmineProjectId
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, UserService, WikiService}

class MappingContextProvider @Inject() (
    projectId: RedmineProjectId,
    issueService: IssueService,
    wikiService: WikiService,
    userService: UserService
) {

  def get(): MappingContext = {
    MappingContext(projectId, issueService, wikiService, userService)
  }

}

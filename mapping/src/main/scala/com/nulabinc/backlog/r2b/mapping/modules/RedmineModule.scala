package com.nulabinc.backlog.r2b.mapping.modules

import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.RedmineDefaultModule

/**
  * @author uchida
  */
private[mapping] class RedmineModule(apiConfig: RedmineApiConfiguration) extends RedmineDefaultModule(apiConfig) {}

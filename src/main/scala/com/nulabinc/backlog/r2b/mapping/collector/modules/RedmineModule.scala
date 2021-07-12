package com.nulabinc.backlog.r2b.mapping.collector.modules

import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.RedmineDefaultModule

/**
 * @author uchida
 */
private[collector] class RedmineModule(apiConfig: RedmineApiConfiguration)
    extends RedmineDefaultModule(apiConfig) {}

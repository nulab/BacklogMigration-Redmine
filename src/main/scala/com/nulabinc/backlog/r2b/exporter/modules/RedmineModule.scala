package com.nulabinc.backlog.r2b.exporter.modules

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogTextFormattingRule}
import com.nulabinc.backlog.r2b.exporter.conf.ExportConfig
import com.nulabinc.backlog.r2b.mapping.core.MappingContainer
import com.nulabinc.backlog.r2b.mapping.service.{MappingStatusService, _}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.RedmineDefaultModule

/**
  * @author uchida
  */
private[exporter] class RedmineModule(apiConfig: RedmineApiConfiguration,
                                      mappingContainer: MappingContainer,
                                      backlogProjectKey: BacklogProjectKey,
                                      backlogTextFormattingRule: BacklogTextFormattingRule,
                                      exportConfig: ExportConfig) extends RedmineDefaultModule(apiConfig) {

  override def configure() = {
    super.configure()
    bind(classOf[BacklogPaths]).toInstance(new BacklogPaths(backlogProjectKey.value))
    bind(classOf[BacklogProjectKey]).toInstance(backlogProjectKey)
    bind(classOf[BacklogTextFormattingRule]).toInstance(backlogTextFormattingRule)

    //mapping service
    bind(classOf[MappingUserService]).toInstance(new MappingUserServiceImpl(mappingContainer.user))
    bind(classOf[MappingPriorityService]).toInstance(new MappingPriorityServiceImpl(mappingContainer.priority))
    bind(classOf[MappingStatusService]).toInstance(new MappingStatusServiceImpl(mappingContainer.status))

    // export
    bind(classOf[ExportConfig]).toInstance(exportConfig)
  }

}

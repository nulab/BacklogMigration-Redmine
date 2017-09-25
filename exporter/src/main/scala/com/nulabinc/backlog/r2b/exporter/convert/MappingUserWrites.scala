package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.r2b.mapping.domain.Mapping

/**
  * @author uchida
  */
private[exporter] class MappingUserWrites @Inject()() extends Writes[Mapping, BacklogUser] {

  override def writes(mapping: Mapping): BacklogUser = {
    BacklogUser(optId = None,
                optUserId = Some(mapping.backlog),
                optPassword = None,
                name = mapping.redmine,
                optMailAddress = None,
                roleType = BacklogConstantValue.USER_ROLE)
  }

}

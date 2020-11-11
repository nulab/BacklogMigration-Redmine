package com.nulabinc.backlog.r2b.exporter.convert

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.domain.mappings.ValidatedUserMapping
import com.nulabinc.backlog.r2b.domain.mappings.RedmineUserMappingItem
import javax.inject.Inject

/**
 * @author uchida
 */
private[exporter] class MappingUserWrites @Inject() ()
    extends Writes[ValidatedUserMapping[RedmineUserMappingItem], BacklogUser] {

  override def writes(mapping: ValidatedUserMapping[RedmineUserMappingItem]): BacklogUser =
    BacklogUser(
      optId = None,
      optUserId = Some(mapping.dst.value),
      optPassword = None,
      name = mapping.src.name,
      optMailAddress = None,
      roleType = BacklogConstantValue.USER_ROLE
    )

}

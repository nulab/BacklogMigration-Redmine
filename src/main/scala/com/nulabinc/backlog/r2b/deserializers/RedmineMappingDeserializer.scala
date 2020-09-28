package com.nulabinc.backlog.r2b.deserializers

import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.mappings.{
  BacklogPriorityMappingItem,
  BacklogStatusMappingItem,
  BacklogUserMappingItem,
  PriorityMapping,
  StatusMapping,
  UserMapping
}
import com.nulabinc.backlog.r2b.domain.mappings.{RedminePriorityMappingItem, RedmineStatusMappingItem, RedmineUserMappingItem}
import org.apache.commons.csv.CSVRecord

object RedmineMappingDeserializer {

  implicit val statusDeserializer: Deserializer[CSVRecord, StatusMapping[RedmineStatusMappingItem]] =
    (record: CSVRecord) =>
      new StatusMapping[RedmineStatusMappingItem] {
        override val src: RedmineStatusMappingItem =
          RedmineStatusMappingItem(record.get(0))
        override val srcDisplayValue: String =
          src.value
        override val optDst: Option[BacklogStatusMappingItem] =
          Option(record.get(1)).map(s => BacklogStatusMappingItem(s))
      }

  implicit val priorityDeserializer: Deserializer[CSVRecord, PriorityMapping[RedminePriorityMappingItem]] =
    (record: CSVRecord) =>
      new PriorityMapping[RedminePriorityMappingItem] {
        override val src: RedminePriorityMappingItem = RedminePriorityMappingItem(
          record.get(0)
        )
        override val srcDisplayValue: String =
          src.value
        override val optDst: Option[BacklogPriorityMappingItem] =
          Option(record.get(1)).map(p => BacklogPriorityMappingItem(p))
      }

  implicit val userDeserializer: Deserializer[CSVRecord, UserMapping[RedmineUserMappingItem]] =
    (record: CSVRecord) =>
      new UserMapping[RedmineUserMappingItem] {
        override val src: RedmineUserMappingItem =
          RedmineUserMappingItem(record.get(0), record.get(1))
        override val srcDisplayValue: String =
          src.displayName
        override val optDst: Option[BacklogUserMappingItem] =
          Option(record.get(2)).map(BacklogUserMappingItem)
        override val mappingType: String =
          record.get(3)
      }
}

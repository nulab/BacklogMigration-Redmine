package com.nulabinc.backlog.r2b.deserializers

import com.nulabinc.backlog.migration.common.deserializers.Deserializer
import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogStatusMappingItem, StatusMapping}
import com.nulabinc.backlog.r2b.domain.mappings.RedmineStatusMappingItem
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
}

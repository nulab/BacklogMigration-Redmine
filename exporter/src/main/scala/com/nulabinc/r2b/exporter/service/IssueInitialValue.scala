package com.nulabinc.r2b.exporter.service

import com.taskadapter.redmineapi.bean.{Journal, JournalDetail}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssueInitialValue(property: String, name: String) {

  def findJournalDetail(journals: Seq[Journal]): Option[JournalDetail] =
    journals.find(isTargetjournal).flatMap(targetJournalDetail)

  def findJournalDetails(journals: Seq[Journal]): Seq[JournalDetail] =
    journals.find(isTargetjournal).map(targetJournalDetails).getOrElse(Seq.empty[JournalDetail])

  private[this] def targetJournalDetail(journal: Journal): Option[JournalDetail] =
    journal.getDetails.asScala.find(isTargetJournalDetail)

  private[this] def isTargetjournal(journal: Journal): Boolean =
    journal.getDetails.asScala.exists(isTargetJournalDetail)

  private[this] def isTargetJournalDetail(detail: JournalDetail): Boolean =
    detail.getName == name && detail.getProperty == property

  private[this] def targetJournalDetails(journal: Journal): Seq[JournalDetail] =
    journal.getDetails.asScala.filter(isTargetJournalDetail).filter(detail => Option(detail.getOldValue).isDefined)

}

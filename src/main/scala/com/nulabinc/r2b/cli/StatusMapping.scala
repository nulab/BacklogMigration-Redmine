package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.Status
import com.nulabinc.r2b.actor.prepare.PrepareData
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.domain.MappingItem
import com.nulabinc.r2b.service.{BacklogService, RedmineService}
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class StatusMapping(conf: R2BConfig, prepareData: PrepareData) extends MappingManager {

  private val backlogDatas = loadBacklog()
  private val redmineDatas = loadRedmine()

  private def loadRedmine(): Seq[MappingItem] = {
    info("- " + Messages("mapping.load_redmine", itemName))
    val redmineService: RedmineService = new RedmineService(conf)
    val redmineStatuses = redmineService.getStatuses()
    val redmines: Seq[MappingItem] = redmineStatuses.map(redmineStatus => MappingItem(redmineStatus.getName, redmineStatus.getName))
    redmines
  }

  private def loadBacklog(): Seq[MappingItem] = {
    info("- " + Messages("mapping.load_backlog", itemName))
    val backlogService: BacklogService = new BacklogService(BacklogConfig(conf.backlogUrl, conf.backlogKey))
    val backlogStatuses: Seq[Status] = backlogService.getStatuses
    val backlogs: Seq[MappingItem] = backlogStatuses.map(backlogStatus => MappingItem(backlogStatus.getName, backlogStatus.getName))
    backlogs
  }

  private object Backlog {
    val OPEN_JA: String = "未対応"
    val IN_PROGRESS_JA: String = "処理中"
    val RESOLVED_JA: String = "処理済み"
    val CLOSED_JA: String = "完了"
    val OPEN_EN: String = "Open"
    val IN_PROGRESS_EN: String = "In Progress"
    val RESOLVED_EN: String = "Resolved"
    val CLOSED_EN: String = "Closed"

    def open(): String = backlogs.map(_.name).find(_ == OPEN_JA).getOrElse(backlogs.map(_.name).find(_ == OPEN_EN).getOrElse(""))

    def inProgress(): String = backlogs.map(_.name).find(_ == IN_PROGRESS_JA).getOrElse(backlogs.map(_.name).find(_ == IN_PROGRESS_EN).getOrElse(""))

    def resolved(): String = backlogs.map(_.name).find(_ == RESOLVED_JA).getOrElse(backlogs.map(_.name).find(_ == RESOLVED_EN).getOrElse(""))

    def closed(): String = backlogs.map(_.name).find(_ == CLOSED_JA).getOrElse(backlogs.map(_.name).find(_ == CLOSED_EN).getOrElse(""))
  }

  private object Redmine {
    val NEW_JA: String = "新規"
    val IN_PROGRESS_JA: String = "進行中"
    val RESOLVED_JA: String = "解決"
    val FEEDBACK_JA: String = "フィードバック"
    val CLOSED_JA: String = "終了"
    val REJECTED_JA: String = "却下"
    val NEW_EN: String = "New"
    val IN_PROGRESS_EN: String = "InProgress"
    val RESOLVED_EN: String = "Resolved"
    val FEEDBACK_EN: String = "Feedback"
    val CLOSED_EN: String = "Closed"
    val REJECTED_EN: String = "Rejected"
  }

  override def matchWithBacklog(redmine: MappingItem): String = {
    val option: Option[String] = backlogs.map(_.name).find(_ == redmine)
    option match {
      case Some(backlog) => backlog
      case None => redmine.name match {
        case Redmine.NEW_JA | Redmine.NEW_EN => Backlog.open()
        case Redmine.IN_PROGRESS_JA | Redmine.IN_PROGRESS_EN => Backlog.inProgress()
        case Redmine.RESOLVED_JA | Redmine.RESOLVED_EN => Backlog.resolved()
        case Redmine.FEEDBACK_JA | Redmine.FEEDBACK_EN => ""
        case Redmine.CLOSED_JA | Redmine.CLOSED_EN => Backlog.closed()
        case Redmine.REJECTED_JA | Redmine.REJECTED_EN => ""
        case _ => ""
      }
    }
  }

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = ConfigBase.STATUS_MAPPING_FILE

  override def itemName: String = Messages("statuses")

  override def description: String =
    Messages("mapping.possible_values", itemName, backlogs.map(_.name).mkString(","))

  override def isDisplayDetail: Boolean = false

}
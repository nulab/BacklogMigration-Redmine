package com.nulabinc.r2b.mapping.core

import com.nulabinc.backlog.migration.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.service.{StatusService => BacklogStatusService}
import com.nulabinc.backlog.migration.utils.StringUtil
import com.nulabinc.backlog4j.Status
import com.nulabinc.r2b.mapping.domain.MappingItem
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.r2b.redmine.service.{StatusService => RedmineStatusService}
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class StatusMappingFile(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration, mappingData: MappingData)
    extends MappingFile {

  private[this] val backlogDatas = loadBacklog()
  private[this] val redmineDatas = loadRedmine()

  private[this] def loadRedmine(): Seq[MappingItem] = {
    val injector                   = RedmineInjector.createInjector(redmineApiConfig)
    val statusService              = injector.getInstance(classOf[RedmineStatusService])
    val redmineStatuses            = statusService.allStatuses()
    val redmines: Seq[MappingItem] = redmineStatuses.map(redmineStatus => MappingItem(redmineStatus.getName, redmineStatus.getName))
    val deleteItems = mappingData.statuses.foldLeft(Seq.empty[MappingItem]) { (acc: Seq[MappingItem], status: String) =>
      {
        val exists = redmineStatuses.exists(redmineStatuse => StringUtil.safeEquals(redmineStatuse.getId.intValue(), status))
        if (exists) acc
        else {
          val name = Messages("cli.mapping.delete_status", status)
          acc :+ MappingItem(name, name)
        }
      }
    }
    redmines union deleteItems
  }

  private[this] def loadBacklog(): Seq[MappingItem] = {
    val injector                     = BacklogInjector.createInjector(backlogApiConfig)
    val statusService                = injector.getInstance(classOf[BacklogStatusService])
    val backlogStatuses: Seq[Status] = statusService.allStatuses()
    val backlogs: Seq[MappingItem]   = backlogStatuses.map(backlogStatus => MappingItem(backlogStatus.getName, backlogStatus.getName))
    backlogs
  }

  private[this] object Backlog {
    val OPEN_JA: String        = "未対応"
    val IN_PROGRESS_JA: String = "処理中"
    val RESOLVED_JA: String    = "処理済み"
    val CLOSED_JA: String      = "完了"
    val OPEN_EN: String        = "Open"
    val IN_PROGRESS_EN: String = "In Progress"
    val RESOLVED_EN: String    = "Resolved"
    val CLOSED_EN: String      = "Closed"

    def open(): String = backlogs.map(_.name).find(_ == OPEN_JA).getOrElse(backlogs.map(_.name).find(_ == OPEN_EN).getOrElse(""))

    def inProgress(): String = backlogs.map(_.name).find(_ == IN_PROGRESS_JA).getOrElse(backlogs.map(_.name).find(_ == IN_PROGRESS_EN).getOrElse(""))

    def resolved(): String = backlogs.map(_.name).find(_ == RESOLVED_JA).getOrElse(backlogs.map(_.name).find(_ == RESOLVED_EN).getOrElse(""))

    def closed(): String = backlogs.map(_.name).find(_ == CLOSED_JA).getOrElse(backlogs.map(_.name).find(_ == CLOSED_EN).getOrElse(""))
  }

  private[this] object Redmine {
    val NEW_JA: String         = "新規"
    val IN_PROGRESS_JA: String = "進行中"
    val RESOLVED_JA: String    = "解決"
    val FEEDBACK_JA: String    = "フィードバック"
    val CLOSED_JA: String      = "終了"
    val REJECTED_JA: String    = "却下"
    val NEW_EN: String         = "New"
    val IN_PROGRESS_EN: String = "InProgress"
    val RESOLVED_EN: String    = "Resolved"
    val FEEDBACK_EN: String    = "Feedback"
    val CLOSED_EN: String      = "Closed"
    val REJECTED_EN: String    = "Rejected"
  }

  override def matchWithBacklog(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name) match {
      case Some(backlog) => backlog
      case None =>
        redmine.name match {
          case Redmine.NEW_JA | Redmine.NEW_EN                 => Backlog.open()
          case Redmine.IN_PROGRESS_JA | Redmine.IN_PROGRESS_EN => Backlog.inProgress()
          case Redmine.RESOLVED_JA | Redmine.RESOLVED_EN       => Backlog.resolved()
          case Redmine.FEEDBACK_JA | Redmine.FEEDBACK_EN       => ""
          case Redmine.CLOSED_JA | Redmine.CLOSED_EN           => Backlog.closed()
          case Redmine.REJECTED_JA | Redmine.REJECTED_EN       => ""
          case _                                               => ""
        }
    }

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = MappingDirectory.STATUS_MAPPING_FILE

  override def itemName: String = Messages("common.statuses")

  override def description: String =
    Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))

  override def isDisplayDetail: Boolean = false

}

package com.nulabinc.backlog.r2b.mapping.file

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{StatusService => BacklogStatusService}
import com.nulabinc.backlog.migration.common.utils.{Logging, StringUtil}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{StatusService => RedmineStatusService}
import com.osinka.i18n.{Lang, Messages}
import com.taskadapter.redmineapi.bean.IssueStatus

/**
 * @author
 *   uchida
 */
class StatusMappingFile(
    redmineApiConfig: RedmineApiConfiguration,
    backlogApiConfig: BacklogApiConfiguration,
    statuses: Seq[String]
) extends Logging {

  private[this] val redmineItems = getRedmineItems
  private[this] val backlogItems = getBacklogItems

  private[this] def getRedmineItems: Seq[MappingItem] = {

    val injector        = RedmineInjector.createInjector(redmineApiConfig)
    val statusService   = injector.getInstance(classOf[RedmineStatusService])
    val redmineStatuses = statusService.allStatuses()

    def createItem(status: IssueStatus): MappingItem = {
      MappingItem(status.getName, status.getName)
    }

    def condition(target: String)(status: IssueStatus): Boolean = {
      StringUtil.safeEquals(status.getId.intValue(), target)
    }

    def collectItems(
        acc: Seq[MappingItem],
        status: String
    ): Seq[MappingItem] = {
      if (redmineStatuses.exists(condition(status))) acc
      else
        acc :+ MappingItem(
          Messages("cli.mapping.delete_status", status),
          Messages("cli.mapping.delete_status", status)
        )
    }

    val redmines    = redmineStatuses.map(createItem)
    val deleteItems = statuses.foldLeft(Seq.empty[MappingItem])(collectItems)
    redmines concat deleteItems
  }

  private[this] def getBacklogItems: Seq[MappingItem] = {
    val injector        = BacklogInjector.createInjector(backlogApiConfig)
    val statusService   = injector.getInstance(classOf[BacklogStatusService])
    val backlogStatuses = statusService.allStatuses()
    backlogStatuses.map { backlogStatus =>
      MappingItem(backlogStatus.name.trimmed, backlogStatus.name.trimmed)
    }
  }

  private[this] object Backlog {
    val OPEN_JA: String = Messages("mapping.status.backlog.open")(Lang("ja"))
    val IN_PROGRESS_JA: String =
      Messages("mapping.status.backlog.in_progress")(Lang("ja"))
    val RESOLVED_JA: String =
      Messages("mapping.status.backlog.resolved")(Lang("ja"))
    val CLOSED_JA: String =
      Messages("mapping.status.backlog.closed")(Lang("ja"))
    val OPEN_EN: String = Messages("mapping.status.backlog.open")(Lang("en"))
    val IN_PROGRESS_EN: String =
      Messages("mapping.status.backlog.in_progress")(Lang("en"))
    val RESOLVED_EN: String =
      Messages("mapping.status.backlog.resolved")(Lang("en"))
    val CLOSED_EN: String =
      Messages("mapping.status.backlog.closed")(Lang("en"))

    def open(): String =
      backlogs
        .map(_.name)
        .find(_ == OPEN_JA)
        .getOrElse(backlogs.map(_.name).find(_ == OPEN_EN).getOrElse(""))

    def inProgress(): String =
      backlogs
        .map(_.name)
        .find(_ == IN_PROGRESS_JA)
        .getOrElse(backlogs.map(_.name).find(_ == IN_PROGRESS_EN).getOrElse(""))

    def resolved(): String =
      backlogs
        .map(_.name)
        .find(_ == RESOLVED_JA)
        .getOrElse(backlogs.map(_.name).find(_ == RESOLVED_EN).getOrElse(""))

    def closed(): String =
      backlogs
        .map(_.name)
        .find(_ == CLOSED_JA)
        .getOrElse(backlogs.map(_.name).find(_ == CLOSED_EN).getOrElse(""))
  }

  private[this] object Redmine {
    val NEW_JA: String = Messages("mapping.status.redmine.new")(Lang("ja"))
    val IN_PROGRESS_JA: String =
      Messages("mapping.status.redmine.in_progress")(Lang("ja"))
    val RESOLVED_JA: String =
      Messages("mapping.status.redmine.resolved")(Lang("ja"))
    val FEEDBACK_JA: String =
      Messages("mapping.status.redmine.feedback")(Lang("ja"))
    val CLOSED_JA: String =
      Messages("mapping.status.redmine.closed")(Lang("ja"))
    val REJECTED_JA: String =
      Messages("mapping.status.redmine.rejected")(Lang("ja"))
    val NEW_EN: String = Messages("mapping.status.redmine.new")(Lang("en"))
    val IN_PROGRESS_EN: String =
      Messages("mapping.status.redmine.in_progress")(Lang("en"))
    val RESOLVED_EN: String =
      Messages("mapping.status.redmine.resolved")(Lang("en"))
    val FEEDBACK_EN: String =
      Messages("mapping.status.redmine.feedback")(Lang("en"))
    val CLOSED_EN: String =
      Messages("mapping.status.redmine.closed")(Lang("en"))
    val REJECTED_EN: String =
      Messages("mapping.status.redmine.rejected")(Lang("en"))
  }

  def matchItem(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name) match {
      case Some(backlog) => backlog
      case None =>
        redmine.name match {
          case Redmine.NEW_JA | Redmine.NEW_EN => Backlog.open()
          case Redmine.IN_PROGRESS_JA | Redmine.IN_PROGRESS_EN =>
            Backlog.inProgress()
          case Redmine.RESOLVED_JA | Redmine.RESOLVED_EN => Backlog.resolved()
          case Redmine.FEEDBACK_JA | Redmine.FEEDBACK_EN => ""
          case Redmine.CLOSED_JA | Redmine.CLOSED_EN     => Backlog.closed()
          case Redmine.REJECTED_JA | Redmine.REJECTED_EN => ""
          case _                                         => ""
        }
    }

  def redmines: Seq[MappingItem] = redmineItems

  def backlogs: Seq[MappingItem] = backlogItems

  def itemName: String = Messages("common.statuses")

  def description: String =
    Messages(
      "cli.mapping.configurable",
      itemName,
      backlogs.map(_.name).mkString(",")
    )

  def isDisplayDetail: Boolean = false

}

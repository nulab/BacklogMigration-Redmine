package com.nulabinc.backlog.r2b.mapping.file

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.common.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.common.service.{PriorityService => BacklogPriorityService}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.backlog.r2b.redmine.service.{PriorityService => RedminePriorityService}
import com.nulabinc.backlog4j.Priority
import com.osinka.i18n.{Lang, Messages}
import com.taskadapter.redmineapi.bean.IssuePriority

/**
 * @author uchida
 */
class PriorityMappingFile(
    redmineApiConfig: RedmineApiConfiguration,
    backlogApiConfig: BacklogApiConfiguration
) extends Logging {

  private[this] val redmineItems = getRedmineItems()
  private[this] val backlogItems = getBacklogItems()

  private[this] def getRedmineItems(): Seq[MappingItem] = {
    def createItem(priority: IssuePriority): MappingItem = {
      MappingItem(priority.getName, priority.getName)
    }

    val injector          = RedmineInjector.createInjector(redmineApiConfig)
    val priorityService   = injector.getInstance(classOf[RedminePriorityService])
    val redminePriorities = priorityService.allPriorities()
    redminePriorities.map(createItem)
  }

  private[this] def getBacklogItems(): Seq[MappingItem] = {
    def createItem(priority: Priority): MappingItem = {
      MappingItem(priority.getName, priority.getName)
    }
    val injector          = BacklogInjector.createInjector(backlogApiConfig)
    val priorityService   = injector.getInstance(classOf[BacklogPriorityService])
    val backlogPriorities = priorityService.allPriorities()
    backlogPriorities.map(createItem)
  }

  private object Backlog {
    val LOW_JA: String = Messages("mapping.priority.backlog.low")(Lang("ja"))
    val NORMAL_JA: String =
      Messages("mapping.priority.backlog.normal")(Lang("ja"))
    val HIGH_JA: String = Messages("mapping.priority.backlog.high")(Lang("ja"))
    val LOW_EN: String  = Messages("mapping.priority.backlog.low")(Lang("en"))
    val NORMAL_EN: String =
      Messages("mapping.priority.backlog.normal")(Lang("en"))
    val HIGH_EN: String = Messages("mapping.priority.backlog.high")(Lang("en"))

    def low(): String =
      backlogs
        .map(_.name)
        .find(_ == LOW_JA)
        .getOrElse(backlogs.map(_.name).find(_ == LOW_EN).getOrElse(""))

    def normal(): String =
      backlogs
        .map(_.name)
        .find(_ == NORMAL_JA)
        .getOrElse(backlogs.map(_.name).find(_ == NORMAL_EN).getOrElse(""))

    def high(): String =
      backlogs
        .map(_.name)
        .find(_ == HIGH_JA)
        .getOrElse(backlogs.map(_.name).find(_ == HIGH_EN).getOrElse(""))
  }

  private object Redmine {
    val LOW_JA: String = Messages("mapping.priority.redmine.low")(Lang("ja"))
    val NORMAL_JA: String =
      Messages("mapping.priority.redmine.normal")(Lang("ja"))
    val HIGH_JA: String = Messages("mapping.priority.redmine.high")(Lang("ja"))
    val URGENT_JA: String =
      Messages("mapping.priority.redmine.urgent")(Lang("ja"))
    val IMMEDIATE_JA: String =
      Messages("mapping.priority.redmine.immediate")(Lang("ja"))
    val LOW_EN: String = Messages("mapping.priority.redmine.low")(Lang("en"))
    val NORMAL_EN: String =
      Messages("mapping.priority.redmine.normal")(Lang("en"))
    val HIGH_EN: String = Messages("mapping.priority.redmine.high")(Lang("en"))
    val URGENT_EN: String =
      Messages("mapping.priority.redmine.urgent")(Lang("en"))
    val IMMEDIATE_EN: String =
      Messages("mapping.priority.redmine.immediate")(Lang("en"))
  }

  def matchItem(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name) match {
      case Some(backlog) => backlog
      case None =>
        redmine.name match {
          case Redmine.LOW_JA | Redmine.LOW_EN             => Backlog.low()
          case Redmine.NORMAL_JA | Redmine.NORMAL_EN       => Backlog.normal()
          case Redmine.HIGH_JA | Redmine.HIGH_EN           => Backlog.high()
          case Redmine.URGENT_JA | Redmine.URGENT_EN       => ""
          case Redmine.IMMEDIATE_JA | Redmine.IMMEDIATE_EN => ""
          case _                                           => ""
        }
    }

  def redmines: Seq[MappingItem] = redmineItems

  def backlogs: Seq[MappingItem] = backlogItems

  def itemName: String = Messages("common.priorities")

  def description: String =
    Messages(
      "cli.mapping.configurable",
      itemName,
      backlogs.map(_.name).mkString(",")
    )

  def isDisplayDetail: Boolean = false

}

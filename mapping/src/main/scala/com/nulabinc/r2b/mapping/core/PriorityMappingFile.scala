package com.nulabinc.r2b.mapping.core

import com.nulabinc.backlog.migration.conf.BacklogApiConfiguration
import com.nulabinc.backlog.migration.modules.{ServiceInjector => BacklogInjector}
import com.nulabinc.backlog.migration.service.{PriorityService => BacklogPriorityService}
import com.nulabinc.backlog4j.Priority
import com.nulabinc.r2b.mapping.domain.MappingItem
import com.nulabinc.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.r2b.redmine.modules.{ServiceInjector => RedmineInjector}
import com.nulabinc.r2b.redmine.service.{PriorityService => RedminePriorityService}
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class PriorityMappingFile(redmineApiConfig: RedmineApiConfiguration, backlogApiConfig: BacklogApiConfiguration) extends MappingFile {

  private[this] val backlogDatas = loadBacklog()
  private[this] val redmineDatas = loadRedmine()

  private[this] def loadRedmine(): Seq[MappingItem] = {
    val injector                   = RedmineInjector.createInjector(redmineApiConfig)
    val priorityService            = injector.getInstance(classOf[RedminePriorityService])
    val redminePriorities          = priorityService.allPriorities()
    val redmines: Seq[MappingItem] = redminePriorities.map(redminePriority => MappingItem(redminePriority.getName, redminePriority.getName))
    redmines
  }

  private[this] def loadBacklog(): Seq[MappingItem] = {
    val injector                         = BacklogInjector.createInjector(backlogApiConfig)
    val priorityService                  = injector.getInstance(classOf[BacklogPriorityService])
    val backlogPriorities: Seq[Priority] = priorityService.allPriorities()
    val backlogs: Seq[MappingItem]       = backlogPriorities.map(backlogPriority => MappingItem(backlogPriority.getName, backlogPriority.getName))
    backlogs
  }

  private object Backlog {
    val LOW_JA: String    = Messages("mapping.priority.backlog.low")(Lang("ja"))
    val NORMAL_JA: String = Messages("mapping.priority.backlog.normal")(Lang("ja"))
    val HIGH_JA: String   = Messages("mapping.priority.backlog.high")(Lang("ja"))
    val LOW_EN: String    = Messages("mapping.priority.backlog.low")(Lang("en"))
    val NORMAL_EN: String = Messages("mapping.priority.backlog.normal")(Lang("en"))
    val HIGH_EN: String   = Messages("mapping.priority.backlog.high")(Lang("en"))

    def low(): String = backlogs.map(_.name).find(_ == LOW_JA).getOrElse(backlogs.map(_.name).find(_ == LOW_EN).getOrElse(""))

    def normal(): String = backlogs.map(_.name).find(_ == NORMAL_JA).getOrElse(backlogs.map(_.name).find(_ == NORMAL_EN).getOrElse(""))

    def high(): String = backlogs.map(_.name).find(_ == HIGH_JA).getOrElse(backlogs.map(_.name).find(_ == HIGH_EN).getOrElse(""))
  }

  private object Redmine {
    val LOW_JA: String       = Messages("mapping.priority.redmine.low")(Lang("ja"))
    val NORMAL_JA: String    = Messages("mapping.priority.redmine.normal")(Lang("ja"))
    val HIGH_JA: String      = Messages("mapping.priority.redmine.high")(Lang("ja"))
    val URGENT_JA: String    = Messages("mapping.priority.redmine.urgent")(Lang("ja"))
    val IMMEDIATE_JA: String = Messages("mapping.priority.redmine.immediate")(Lang("ja"))
    val LOW_EN: String       = Messages("mapping.priority.redmine.low")(Lang("en"))
    val NORMAL_EN: String    = Messages("mapping.priority.redmine.normal")(Lang("en"))
    val HIGH_EN: String      = Messages("mapping.priority.redmine.high")(Lang("en"))
    val URGENT_EN: String    = Messages("mapping.priority.redmine.urgent")(Lang("en"))
    val IMMEDIATE_EN: String = Messages("mapping.priority.redmine.immediate")(Lang("en"))
  }

  override def matchWithBacklog(redmine: MappingItem): String =
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

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = MappingDirectory.PRIORITY_MAPPING_FILE

  override def itemName: String = Messages("common.priorities")

  override def description: String =
    Messages("cli.mapping.configurable", itemName, backlogs.map(_.name).mkString(","))

  override def isDisplayDetail: Boolean = false

}

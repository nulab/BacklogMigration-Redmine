package com.nulabinc.backlog.r2b.mapping.file

import java.util.Locale

import com.nulabinc.backlog.r2b.mapping.domain.Mapping
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
private[file] class MappingValidator(redmineMappings: Seq[MappingItem], backlogMappings: Seq[MappingItem], itemName: String) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val CHECK_REDMINE = "CHECK_REDMINE"
  val CHECK_BACKLOG = "CHECK_BACKLOG"

  def validate(optMappings: Option[Seq[Mapping]]): Seq[String] = {
    optMappings match {
      case Some(mappings) =>
        itemsExists(mappings, CHECK_REDMINE) concat
          itemsRequired(mappings, CHECK_REDMINE) concat
          itemsExists(mappings, CHECK_BACKLOG) concat
          itemsRequired(mappings, CHECK_BACKLOG)
      case _ => throw new RuntimeException
    }
  }

  private[this] def itemsExists(mappings: Seq[Mapping], checkService: String): Seq[String] = {
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) =>
      if (checkService == CHECK_REDMINE) {
        itemExists(mapping.redmine, redmineMappings, Messages("common.src")) match {
          case Some(error) => errors :+ error
          case None        => errors
        }
      } else {
        itemExists(mapping.backlog, backlogMappings, Messages("common.dst")) match {
          case Some(error) => errors :+ error
          case None        => errors
        }
    })
  }

  private[this] def itemExists(value: String, mappingItems: Seq[MappingItem], serviceName: String): Option[String] = {
    if (value.nonEmpty && !mappingItems.exists(_.name == value)) {
      Some(s"- ${Messages("cli.mapping.error.not_exist.item", itemName, value, serviceName)}")
    } else None
  }

  private[this] def itemsRequired(mappings: Seq[Mapping], checkService: String): Seq[String] = {
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
      itemRequired(mapping, checkService) match {
        case Some(error) => errors :+ error
        case None        => errors
      }
    })
  }

  private[this] def itemRequired(mapping: Mapping, checkService: String): Option[String] = {
    if (checkService == CHECK_REDMINE) {
      if (mapping.redmine.isEmpty) Some(s"- ${Messages("cli.mapping.error.empty.item", Messages("common.dst"), itemName, mapping.backlog)}")
      else None
    } else {
      if (mapping.backlog.isEmpty) Some(s"- ${Messages("cli.mapping.error.empty.item", Messages("common.dst"), itemName, mapping.redmine)}")
      else None
    }
  }

}

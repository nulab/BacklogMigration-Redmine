package com.nulabinc.backlog.r2b.mapping.file

import java.util.Locale

import com.nulabinc.backlog.r2b.mapping.domain.Mapping
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
private[file] class MappingValidator(redmineMappings: Seq[MappingItem], backlogMappings: Seq[MappingItem], itemName: String, fileName: String) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def validate(optMappings: Option[Seq[Mapping]]): Seq[String] = {
    optMappings match {
      case Some(mappings) =>
        itemsExists(mappings) union
          itemsRequired(mappings) union
          redmineItemsExists(mappings)
      case _ => throw new RuntimeException
    }
  }

  private[this] def redmineItemsExists(mappings: Seq[Mapping]): Seq[String] = {
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) =>
      redmineItemExists(mapping, redmineMappings) match {
        case Some(error) => errors :+ error
        case None        => errors
    })
  }

  private[this] def redmineItemExists(mapping: Mapping, mappingItems: Seq[MappingItem]): Option[String] = {
    if (!mappingItems.exists(mappingItem => mappingItem.name == mapping.redmine)) {
      Some(s"- ${Messages("cli.mapping.error.not_exist.item", itemName, mapping.redmine, Messages("common.redmine"))}")
    } else None
  }

  private[this] def itemsExists(mappings: Seq[Mapping]): Seq[String] = {
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
      itemExists(mapping) match {
        case Some(error) => errors :+ error
        case None        => errors
      }
    })
  }

  private[this] def itemExists(mapping: Mapping): Option[String] = {
    if (mapping.backlog.nonEmpty && !backlogMappings.exists(_.name == mapping.backlog)) {
      Some(s"- ${Messages("cli.mapping.error.not_exist.item", itemName, mapping.backlog, Messages("common.backlog"))}")
    } else None
  }

  private[this] def itemsRequired(mappings: Seq[Mapping]): Seq[String] = {
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
      itemRequired(mapping) match {
        case Some(error) => errors :+ error
        case None        => errors
      }
    })
  }

  private[this] def itemRequired(mapping: Mapping): Option[String] = {
    if (mapping.backlog.isEmpty) Some("- " + Messages("cli.mapping.error.empty.item", itemName, mapping.redmine))
    else None
  }

}

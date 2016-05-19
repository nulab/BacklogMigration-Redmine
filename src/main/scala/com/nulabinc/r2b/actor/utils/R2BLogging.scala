package com.nulabinc.r2b.actor.utils

/**
  * @author uchida
  */

import java.util.Locale

import com.nulabinc.backlog4j.BacklogAPIException
import com.osinka.i18n.{Lang, Messages}
import com.taskadapter.redmineapi.RedmineTransportException

trait R2BLogging extends Logging {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val TOP = "top"
  val BOTTOM = "bottom"

  def error(e: Throwable) = {
    val message = e match {
      case bae: BacklogAPIException => bae.getMessage
      case rte: RedmineTransportException => rte.getMessage
      case e: Throwable => Messages("error.unknown")
      case _ => ""
    }
    log.error(s"$message", e)
  }

  def info(message: String) = log.info(message)

  def title(message: String, position: String) =
    if (position == TOP) {
      info(message)
      separator()
    } else if (position == BOTTOM) {
      separator()
      info(message)
    }

  def separator() = info("--------------------------------------------------")

  def separatorln() = {
    separator()
    newLine()
  }

  def newLine() = info("")

}
package com.nulabinc.r2b.actor.utils

/**
 * @author uchida
 */

import java.util.Locale

import akka.actor.{Actor, ActorLogging}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.r2b.conf.ConfigBase
import com.osinka.i18n.{Messages, Lang}
import com.taskadapter.redmineapi.RedmineTransportException

trait R2BLogging extends Actor with ActorLogging {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val label = ConfigBase.LOG_APPLICATION_LABEL
  val name = self.path.name

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    log.warning(s"${label}preRestart: reason: $reason, message: $message")
    super.preRestart(reason, message)
  }

  //  override def preStart() = {
  //    printlog(s"$label[$name]Start")
  //    super.preStart()
  //  }
  //
  //  override def postStop() = {
  //    printlog(s"$label[$name]Stop")
  //    super.postStop()
  //  }

  def errorLog(e: Throwable) = {
    val message = e match {
      case bae: BacklogAPIException => bae.getMessage
      case rte: RedmineTransportException => rte.getMessage
      case e: Throwable => Messages("error.unknown")
      case _ => ""
    }
    log.error(e, s"$message")
  }

  def printlog(message: String) = {
    println(message)
    log.info(message)
  }

  def printlog() = {
    println("")
    log.info("")
  }

}
package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, _}
import akka.util.Timeout
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.domain.{BacklogGroupsWrapper, BacklogJsonProtocol, BacklogUsersWrapper}
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.{ConvertService, RedmineUnmarshaller}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * @author uchida
  */
class ConvertActor(r2bConf: R2BConfig) extends Actor with R2BLogging with Subtasks {

  import BacklogJsonProtocol._

  override val supervisorStrategy = AllForOneStrategy(maxNrOfRetries = 0) {
    case e: Exception =>
      error(e)
      context.system.shutdown()
      Stop
  }

  def receive: Receive = {
    case ConvertActor.Do =>
      title(Messages("message.start_convert"), TOP)

      groups()

      start(Props(new ProjectsActor(r2bConf)), ProjectsActor.actorName) ! ProjectsActor.Do

    case Terminated(ref) =>
      complete(ref)
      if (subtasks.isEmpty) {
        title(Messages("message.completed_convert"), BOTTOM)
        newLine()
        newLine()
        context.system.shutdown()
      }
  }

  private def groups() = {
    for {redmineUsers <- RedmineUnmarshaller.users()
         redmineGroups <- RedmineUnmarshaller.groups()} yield {

      info(Messages("message.start_groups_convert"))

      val backlogGroupsWrapper: BacklogGroupsWrapper = ConvertService.Groups(redmineGroups, redmineUsers)
      IOUtil.output(BacklogConfigBase.Backlog.GROUPS, backlogGroupsWrapper.toJson.prettyPrint)
    }
  }

}

object ConvertActor {
  implicit val timeout = Timeout(60 minutes)

  case class Do()

  def actorName = s"ConvertActor_$randomUUID"

  def apply(r2bConf: R2BConfig) = {
    val system = ActorSystem("convert")
    val actor = system.actorOf(Props(new ConvertActor(r2bConf)), ConvertActor.actorName)
    actor ! ConvertActor.Do
    system.awaitTermination(timeout.duration)
  }

}

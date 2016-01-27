package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.conf.ConfigBase.Redmine
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.{RedmineMarshaller, RedmineService}
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{News, Project, User}

/**
 * @author uchida
 */
class NewsActor(conf: R2BConfig, project: Project) extends Actor with R2BLogging {

  def receive: Receive = {
    case NewsActor.Do =>
      info(Messages("message.execute_redmine_news_export", project.getName))
      val redmineService: RedmineService = new RedmineService(conf)
      val news: Seq[News] = redmineService.getNews(project.getIdentifier)
      val users: Seq[User] = redmineService.getUsers
      IOUtil.output(Redmine.getNewsPath(project.getIdentifier), RedmineMarshaller.News(news, users))
      context.stop(self)
  }

}

object NewsActor {

  case class Do()

  def actorName = s"NewsActor_$randomUUID"

}


package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject
import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{BacklogAttachment, BacklogSharedFile, BacklogTextFormattingRule, BacklogWiki}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, Logging}
import com.nulabinc.backlog.r2b.utils.TextileUtil
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.News

/**
  * @author uchida
  */
private[exporter] class NewsWrites @Inject()(implicit val userWrites: UserWrites, backlogTextFormattingRule: BacklogTextFormattingRule) extends Writes[News, BacklogWiki] with Logging {
  override def writes(news: News): BacklogWiki = {
    BacklogWiki(
      optId = None,
      name = s"${Messages("common.news")}/${Option(news.getTitle).getOrElse("")}",
      optContent = Some(content(news)),
      attachments = Seq.empty[BacklogAttachment],
      sharedFiles = Seq.empty[BacklogSharedFile],
      optCreatedUser = Option(news.getUser).map(Convert.toBacklog(_)),
      optCreated = Option(news.getCreatedOn).map(DateUtil.isoFormat),
      optUpdatedUser = Option(news.getUser).map(Convert.toBacklog(_)),
      optUpdated = Option(news.getCreatedOn).map(DateUtil.isoFormat)
    )
  }

  private[this] def content(news: News): String = {
    val sb = new StringBuilder
    sb.append(news.getDescription)
    for { link <- Option(news.getLink) } yield {
      sb.append("\n\n\n")
      sb.append(Messages("common.link")).append(":").append(link)
    }
    TextileUtil.convert(sb.toString(), backlogTextFormattingRule)
  }

}

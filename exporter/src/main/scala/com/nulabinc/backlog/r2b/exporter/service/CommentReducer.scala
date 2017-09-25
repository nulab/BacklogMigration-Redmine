package com.nulabinc.backlog.r2b.exporter.service

import java.io.{FileOutputStream, InputStream}
import java.net.URL
import java.nio.channels.Channels

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.domain.{BacklogChangeLog, BacklogComment, BacklogIssue}
import com.nulabinc.backlog.migration.common.utils.{FileUtil, IOUtil, Logging, StringUtil}
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.nulabinc.backlog.r2b.redmine.service.ProjectService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.Attachment

import scalax.file.Path

/**
  * @author uchida
  */
private[exporter] class CommentReducer(exportContext: ExportContext,
                                       projectService: ProjectService,
                                       issueDirPath: Path,
                                       issue: BacklogIssue,
                                       comments: Seq[BacklogComment],
                                       attachments: Seq[Attachment])
    extends Logging {

  private[this] val changeLogContent = new StringBuilder()

  def reduce(comment: BacklogComment): BacklogComment = {
    val newChangeLogs = comment.changeLogs.flatMap(changeLog => parse(comment, changeLog))
    val optNewContent = comment.optContent match {
      case Some(content) =>
        val newContent = (s"${changeLogContent.result()}\n${content}").trim
        StringUtil.notEmpty(newContent)
      case None =>
        StringUtil.notEmpty(changeLogContent.result().trim)
    }
    comment.copy(optIssueId = Some(issue.id), optContent = optNewContent, isCreateIssue = false, changeLogs = newChangeLogs)
  }

  private[this] def parse(comment: BacklogComment, changeLog: BacklogChangeLog): Option[BacklogChangeLog] = {

    def getValue(optValue: Option[String]): String =
      optValue.getOrElse(Messages("common.empty"))

    def getProjectName(optValue: Option[String]): String = {
      optValue match {
        case Some(value) =>
          StringUtil.safeStringToInt(value) match {
            case Some(intValue) => projectService.optProjectOfId(intValue).map(_.getName).getOrElse(Messages("common.empty"))
            case _              => Messages("common.empty")
          }
        case _ => Messages("common.empty")
      }
    }

    def privateValue(optValue: Option[String]): Option[String] =
      optValue match {
        case Some("0") => Some(Messages("common.no"))
        case Some("1") => Some(Messages("common.yes"))
        case _         => None
      }

    changeLog.field match {
      case BacklogConstantValue.ChangeLog.ATTACHMENT => attachment(changeLog)
      case "done_ratio" =>
        changeLogContent
          .append(
            Messages("common.change_comment", Messages("common.done_ratio"), getValue(changeLog.optOriginalValue), getValue(changeLog.optNewValue)))
          .append("\n")
        None
      case "relates" =>
        changeLogContent
          .append(
            Messages("common.change_comment", Messages("common.relation"), getValue(changeLog.optOriginalValue), getValue(changeLog.optNewValue)))
          .append("\n")
        None
      case "is_private" =>
        changeLogContent
          .append(
            Messages("common.change_comment",
                     Messages("common.private"),
                     getValue(privateValue(changeLog.optOriginalValue)),
                     getValue(privateValue(changeLog.optNewValue))))
          .append("\n")
        None
      case "project_id" =>
        changeLogContent
          .append(
            Messages("common.change_comment",
                     Messages("common.project"),
                     getProjectName(changeLog.optOriginalValue),
                     getProjectName(changeLog.optNewValue)))
          .append("\n")
        None
      case _ =>
        Some(changeLog.copy(optNewValue = issuePropertyNewValue(comment, changeLog)))
    }
  }

  private[this] def attachment(changeLog: BacklogChangeLog): Option[BacklogChangeLog] = {
    changeLog.optAttachmentInfo match {
      case Some(attachmentInfo) =>
        val optAttachment = attachments.find(attachment => FileUtil.normalize(attachment.getFileName) == attachmentInfo.name)
        optAttachment match {
          case Some(attachment) =>
            val url: URL = new URL(s"${attachment.getContentURL}?key=${exportContext.apiConfig.key}")
            download(attachmentInfo.name, url.openStream())
            Some(changeLog)
          case _ => None
        }
      case _ => Some(changeLog)
    }
  }

  private[this] def download(name: String, content: InputStream) = {
    val dir  = exportContext.backlogPaths.issueAttachmentDirectoryPath(issueDirPath)
    val path = exportContext.backlogPaths.issueAttachmentPath(dir, name)
    IOUtil.createDirectory(dir)
    val rbc = Channels.newChannel(content)
    val fos = new FileOutputStream(path.path)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

    rbc.close()
    fos.close()
  }

  private[this] def issuePropertyNewValue(comment: BacklogComment, changeLog: BacklogChangeLog): Option[String] =
    changeLog.field match {
      case BacklogConstantValue.ChangeLog.VERSION | BacklogConstantValue.ChangeLog.MILESTONE | BacklogConstantValue.ChangeLog.COMPONENT |
          BacklogConstantValue.ChangeLog.ISSUE_TYPE =>
        val optLastComment: Option[BacklogComment] = findProperty(comments)(changeLog.field)
        optLastComment match {
          case Some(lastComment) if (lastComment.optCreated == comment.optCreated) =>
            changeLog.field match {
              case BacklogConstantValue.ChangeLog.VERSION =>
                val issueValue = issue.versionNames.mkString(", ")
                if (issueValue.trim.isEmpty) changeLog.optNewValue else Some(issueValue)
              case BacklogConstantValue.ChangeLog.MILESTONE =>
                val issueValue = issue.milestoneNames.mkString(", ")
                if (issueValue.trim.isEmpty) changeLog.optNewValue else Some(issueValue)
              case BacklogConstantValue.ChangeLog.COMPONENT =>
                val issueValue = issue.categoryNames.mkString(", ")
                if (issueValue.trim.isEmpty) changeLog.optNewValue else Some(issueValue)
              case BacklogConstantValue.ChangeLog.ISSUE_TYPE =>
                val issueValue = issue.optIssueTypeName.getOrElse("")
                if (issueValue.trim.isEmpty) changeLog.optNewValue else Some(issueValue)
              case _ => throw new RuntimeException
            }
          case _ => changeLog.optNewValue
        }
      case _ => changeLog.optNewValue
    }

  private[this] def findProperty(comments: Seq[BacklogComment])(field: String): Option[BacklogComment] = {
    comments.reverse.find(comment => findProperty(comment)(field))
  }

  private[this] def findProperty(comment: BacklogComment)(field: String): Boolean =
    comment.changeLogs.map(findProperty).exists(_(field))

  private[this] def findProperty(changeLog: BacklogChangeLog)(field: String): Boolean = changeLog.field == field

}

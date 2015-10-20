package com.nulabinc.r2b.actor.convert

import java.util.UUID._

import akka.actor.Actor
import com.nulabinc.backlog.importer.conf.{ConfigBase => BacklogConfigBase}
import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog.importer.domain.{BacklogIssue, BacklogJsonProtocol}
import com.nulabinc.backlog4j.Issue
import com.nulabinc.r2b.actor.utils.{IssueTag, R2BLogging}
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service._
import com.nulabinc.r2b.utils.IOUtil
import com.osinka.i18n.Messages
import spray.json._

import scalax.file.Path

/**
 * @author uchida
 */
class IssuesActor(r2bConf: R2BConfig, projectInfo: ProjectInfo) extends Actor with R2BLogging {

  import BacklogJsonProtocol._

  var issueSize: Int = 0
  var convertCount: Int = 0

  val redmineService: RedmineService = new RedmineService(r2bConf)
  val backlogService: BacklogService = new BacklogService(BacklogConfig(r2bConf.backlogUrl, r2bConf.backlogKey))
  val registeredIssues: Seq[Issue] = backlogService.getIssues(projectInfo.projectKey.backlog)

  val projectEnumerations: ProjectEnumerations = new ProjectEnumerations(projectInfo.projectKey.redmine)

  def receive: Receive = {
    case IssuesActor.Do =>
      val paths: Seq[Path] = IOUtil.directoryPaths(ConfigBase.Redmine.getIssuesDir(projectInfo.projectKey.redmine))
      issueSize = paths.size
      if (issueSize != 0) printlog(Messages("message.execute_issues_convert", projectInfo.name, issueSize))
      paths.foreach(convert)
      context.stop(self)
  }

  private def convert(issuePath: Path) = {
    for {redmineIssue <- RedmineUnmarshaller.issue(issuePath.path + "/" + ConfigBase.ISSUE_FILE_NAME)} yield {
      if (!isRegistered(redmineIssue, registeredIssues)) {

        //Convert and output backlog issue
        val redmineCustomFieldDefinitions: Seq[RedmineCustomFieldDefinition] = RedmineUnmarshaller.customFieldDefinitions().getOrElse(Seq.empty[RedmineCustomFieldDefinition])

        val backlogIssue: BacklogIssue = ConvertService.Issue(projectEnumerations, redmineIssue, redmineCustomFieldDefinitions, r2bConf.redmineUrl)
        IOUtil.output(BacklogConfigBase.Backlog.getIssuePath(projectInfo.projectKey.backlog, redmineIssue.id), backlogIssue.toJson.prettyPrint)

        //Copy attachments
        val redmineAttachments: Seq[RedmineAttachment] = redmineIssue.attachments
        redmineAttachments.foreach(redmineAttachment => copy(redmineAttachment, redmineIssue))

        convertCount += 1
        printlog(Messages("message.execute_issue_convert", projectInfo.name, convertCount, issueSize))
      }
    }
  }

  private def copy(attachment: RedmineAttachment, redmineIssue: RedmineIssue) = {
    val dir: String = ConfigBase.Redmine.getIssueAttachmentDir(projectInfo.projectKey.redmine, redmineIssue.id, attachment.id)

    val redmineFilePath: String = dir + "/" + attachment.fileName
    val convertFilePath: String = BacklogConfigBase.Backlog.getIssueAttachmentDir(projectInfo.projectKey.backlog, redmineIssue.id, attachment.id, attachment.fileName)
    IOUtil.copy(redmineFilePath, convertFilePath)
  }

  private def isRegistered(redmineIssue: RedmineIssue, registeredIssues: Seq[Issue]): Boolean = {
    registeredIssues.exists(issue => IssueTag.isTaged(redmineIssue.id, issue.getDescription, r2bConf.redmineUrl))
  }

}

object IssuesActor {

  case class Do()

  def actorName = s"IssuesActor_$randomUUID"

}
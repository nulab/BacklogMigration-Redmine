package com.nulabinc.backlog.r2b.core

import java.util.Date

import com.nulabinc.backlog.migration.common.utils.FileUtil
import com.nulabinc.backlog.r2b.conf.AppConfiguration
import com.nulabinc.backlog.r2b.helper.SimpleFixture
import com.nulabinc.backlog4j.api.option.{GetIssuesParams, QueryParams}
import com.nulabinc.backlog4j.{IssueComment, Issue => BacklogIssue}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{User, Issue => RedmineIssue}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class R2BSpec extends FlatSpec with Matchers with SimpleFixture {

  for { appConfiguration <- optAppConfiguration } yield {
    testProject(appConfiguration)
    testProjectUsers(appConfiguration)
    testVersion(appConfiguration)
    testTracker(appConfiguration)
    testWikis(appConfiguration)
    testIssues(appConfiguration)
  }

  private[this] def testProject(appConfiguration: AppConfiguration): Unit = {
    "Project" should "match" in {
      val redmineProject = redmine.getProjectManager.getProjectByKey(appConfiguration.redmineConfig.projectKey)
      val backlogProject = backlog.getProject(appConfiguration.backlogConfig.projectKey)

      backlogProject.getName should equal(redmineProject.getName)
      backlogProject.isChartEnabled should be(true)
      backlogProject.isSubtaskingEnabled should be(true)
      backlogProject.getTextFormattingRule should equal(com.nulabinc.backlog4j.Project.TextFormattingRule.Markdown)
    }
  }

  private[this] def testProjectUsers(appConfiguration: AppConfiguration) = {
    "Project user" should "match" in {
      val backlogUsers = backlog.getProjectUsers(appConfiguration.backlogConfig.projectKey).asScala
      val memberShips  = redmine.getMembershipManager.getMemberships(appConfiguration.redmineConfig.projectKey).asScala

      val redmineUsers = memberShips
        .filter(_.getUser != null)
        .map(memberShip => {
          redmine.getUserManager.getUserById(memberShip.getUser.getId)
        })

      redmineUsers.foreach(redmineUser => {
        backlogUsers.exists(backlogUser => {
          backlogUser.getUserId == convertUser(redmineUser.getLogin)
        }) should be(true)
      })
    }
  }

  private[this] def testVersion(appConfiguration: AppConfiguration) = {
    "Version" should "match" in {
      val backlogVersions = backlog.getVersions(appConfiguration.backlogConfig.projectKey).asScala
      val redmineProject  = redmine.getProjectManager.getProjectByKey(appConfiguration.redmineConfig.projectKey)
      val redmineVersions = redmine.getProjectManager.getVersions(redmineProject.getId).asScala
      redmineVersions.foreach(redmineVersion => {
        val optBacklogVersion = backlogVersions.find(backlogVersion => redmineVersion.getName == backlogVersion.getName)
        optBacklogVersion.isDefined should be(true)
        for { backlogVersion <- optBacklogVersion } yield {
          redmineVersion.getName should equal(backlogVersion.getName)
          Option(redmineVersion.getDescription).getOrElse("") should equal(Option(backlogVersion.getDescription).getOrElse(""))
          dateToString(redmineVersion.getDueDate) should equal(dateToString(backlogVersion.getReleaseDueDate))
        }
      })
    }
  }

  private[this] def testTracker(appConfiguration: AppConfiguration) = {
    "Tracker" should "match" in {
      val backlogIssueTypes = backlog.getIssueTypes(appConfiguration.backlogConfig.projectKey).asScala
      val redmineTrackers   = redmine.getIssueManager.getTrackers.asScala
      redmineTrackers.foreach(redmineTracker => {
        val backlogIssueType = backlogIssueTypes.find(backlogIssueType => redmineTracker.getName == backlogIssueType.getName).get
        redmineTracker.getName should equal(backlogIssueType.getName)
      })
    }
  }

  private[this] def testWikis(appConfiguration: AppConfiguration) = {
    def convertTitle(title: String) = {
      if (title == "Wiki") "Home" else title
    }

    val backlogWikis = backlog.getWikis(appConfiguration.backlogConfig.projectKey).asScala.map(backlogWiki => backlog.getWiki(backlogWiki.getId))
    val redmineWikis = redmine.getWikiManager.getWikiPagesByProject(appConfiguration.redmineConfig.projectKey).asScala
    redmineWikis.foreach(wiki =>
      "Wiki" should s"match: ${wiki.getTitle}" in {
        val redmineWiki =
          redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(appConfiguration.redmineConfig.projectKey, wiki.getTitle)
        val optBacklogWiki = backlogWikis.find(convertTitle(redmineWiki.getTitle) == _.getName)

        withClue(s"title:${wiki.getTitle}") {
          optBacklogWiki should not be (None)
        }

        for { backlogWiki <- optBacklogWiki } yield {
          val sb = new StringBuilder
          if (redmineWiki.getText != null) sb.append(redmineWiki.getText)
          if (redmineWiki.getComments != null)
            sb.append("\n\n\n").append(Messages("common.comment")).append(":").append(redmineWiki.getComments)
          if (redmineWiki.getParent != null)
            sb.append("\n").append(Messages("common.parent_page")).append(":[[").append(redmineWiki.getParent.getTitle).append("]]")
          val redmineContent: String = sb.result()

          val redmineWikiUser = redmine.getUserManager.getUserById(redmineWiki.getUser.getId)

          convertTitle(redmineWiki.getTitle) should equal(backlogWiki.getName)
          redmineContent should equal(backlogWiki.getContent)

          withClue(s"""login:${redmineWikiUser.getLogin}
                      |converted:${convertUser(redmineWikiUser.getLogin)}
                      |backlog:${backlogWiki.getCreatedUser.getUserId}""".stripMargin) {
            convertUser(redmineWikiUser.getLogin) should equal(backlogWiki.getCreatedUser.getUserId)
          }
          withClue(s"login:${redmineWikiUser.getLogin} converted:${convertUser(redmineWikiUser.getLogin)}") {
            convertUser(redmineWikiUser.getLogin) should equal(backlogWiki.getUpdatedUser.getUserId)
          }
          if (redmineWiki.getTitle != "Wiki") {
            timestampToString(redmineWiki.getCreatedOn) should equal(timestampToString(backlogWiki.getCreated))
          }
          redmineWiki.getAttachments.asScala.foreach(redmineAttachment => {
            withClue(s"name:${redmineAttachment.getFileName}") {
              backlogWiki.getAttachments.asScala.exists(backlogAttachment => {
                FileUtil.normalize(backlogAttachment.getName) == FileUtil.normalize(redmineAttachment.getFileName)
              }) should be(true)
            }
          })
        }
    })
  }

  private[this] def testIssues(appConfiguration: AppConfiguration) = {
    val allCount = redmineIssueCount(appConfiguration)
    val COUNT    = 100

    (0 until (allCount, COUNT)).foreach(offset => issues(appConfiguration, COUNT, offset))
  }

  private[this] def issues(appConfiguration: AppConfiguration, count: Int, offset: Long) = {
    val backlogProject = backlog.getProject(appConfiguration.backlogConfig.projectKey)
    val params         = new GetIssuesParams(List(Long.box(backlogProject.getId)).asJava)
    val backlogIssues  = backlog.getIssues(params).asScala

    val redmineIssues = allRedmineIssues(count, offset).map(tryIssue)

    redmineIssues.foreach(redmineIssue =>
      "Issue" should s"match: ${redmineIssue.getSubject}[${redmineIssue.getId}]" in {

        val optBacklogIssue = backlogIssues.find(backlogIssue => redmineIssue.getSubject == backlogIssue.getSummary)

        withClue(s"""
            |redmine subject:${redmineIssue.getSubject}
          """.stripMargin) {
          optBacklogIssue should not be (None)
        }

        for { backlogIssue <- optBacklogIssue } yield {
          //description
          redmineIssue.getDescription should equal(backlogIssue.getDescription)

          //issue type
          redmineIssue.getTracker.getName should equal(backlogIssue.getIssueType.getName)

          //category
          for { category <- Option(redmineIssue.getCategory) } yield {
            val optBacklogCategory = if (backlogIssue.getCategory.asScala.isEmpty) None else Some(backlogIssue.getCategory.asScala(0))
            category.getName should equal(optBacklogCategory.map(_.getName).getOrElse(""))
          }

          //milestone
          for { version <- Option(redmineIssue.getTargetVersion) } yield {
            version.getName should equal(backlogIssue.getMilestone.get(0).getName)
          }

          //due date
          (Option(redmineIssue.getDueDate), Option(backlogIssue.getDueDate)) match {
            case (Some(r), Some(b)) =>
              dateToString(r) should equal(dateToString(b))
            case (None, None) =>
              assert(true)
            case (r, b) =>
              r should equal(b)
          }


          //priority
          convertPriority(redmineIssue.getPriorityText) should equal(backlogIssue.getPriority.getName)

          //status
          withClue(s"""
               |status:${redmineIssue.getStatusName}
               |converted:${convertStatus(redmineIssue.getStatusName)}
               |""".stripMargin) {
            convertStatus(redmineIssue.getStatusName) should equal(backlogIssue.getStatus.getName)
          }

          //assignee
          if (redmineIssue.getAssignee != null) {
            val redmineUser = redmine.getUserManager.getUserById(redmineIssue.getAssignee.getId)
            convertUser(redmineUser.getLogin) should equal(backlogIssue.getAssignee.getUserId)
          }

          //actual hours
          val spentHours  = BigDecimal(redmineIssue.getSpentHours.toString).setScale(2, BigDecimal.RoundingMode.HALF_UP)
          val actualHours = BigDecimal(backlogIssue.getActualHours).setScale(2, BigDecimal.RoundingMode.HALF_UP)
          spentHours should equal(actualHours)

          //start date
          dateToString(redmineIssue.getStartDate) should equal(dateToString(backlogIssue.getStartDate))

          //created user
          userIdOfUser(redmineIssue.getAuthor) should equal(backlogIssue.getCreatedUser.getUserId)

          //created
          timestampToString(redmineIssue.getCreatedOn) should equal(timestampToString(backlogIssue.getCreated))

          //updated user
          withClue(s"""
              |redmine:${timestampToString(redmineIssue.getUpdatedOn)}
              |backlog:${timestampToString(updated(backlogIssue))}
            """.stripMargin) {
            //timestampToString(redmineIssue.getUpdatedOn) should be(timestampToString(updated(backlogIssue)))
          }

          //comments
          allCommentsOfIssue(backlogIssue.getId)
//          val comments = allCommentsOfIssue(backlogIssue.getId)
//          redmineIssue.getJournals.asScala.size should equal(comments.size)

        }

    })
  }

  private[this] def allCommentsOfIssue(issueId: Long): Seq[IssueComment] = {
    val allCount = backlog.getIssueCommentCount(issueId)

    def loop(optMinId: Option[Long], comments: Seq[IssueComment], offset: Long): Seq[IssueComment] =
      if (offset < allCount) {
        val queryParams = new QueryParams()
        for { minId <- optMinId } yield {
          queryParams.minId(minId)
        }
        queryParams.count(100)
        queryParams.order(QueryParams.Order.Asc)
        val commentsPart =
          backlog.getIssueComments(issueId, queryParams).asScala
        val optLastId = for { lastComment <- commentsPart.lastOption } yield {
          lastComment.getId
        }
        loop(optLastId, comments union commentsPart, offset + 100)
      } else comments

    loop(None, Seq.empty[IssueComment], 0).sortWith((c1, c2) => c1.getCreated.before(c2.getCreated))
  }

  private[this] def updated(issue: BacklogIssue): Date = {
    val comments = backlog.getIssueComments(issue.getId)
    if (comments.isEmpty) issue.getUpdated
    else {
      val comment = comments.asScala.sortWith((c1, c2) => {
        c1.getUpdated.before(c2.getUpdated)
      })(comments.size() - 1)
      comment.getCreated
    }
  }

  private[this] def tryIssue(issue: RedmineIssue): RedmineIssue = {
    redmine.getIssueManager.getIssueById(issue.getId.intValue(), Include.attachments, Include.journals)
  }

  private[this] def userOfId(id: Int): User = {
    redmine.getUserManager.getUserById(id)
  }

  private[this] def userIdOfUser(user: User): String = {
    convertUser(userOfId(user.getId.intValue()).getLogin)
  }

}

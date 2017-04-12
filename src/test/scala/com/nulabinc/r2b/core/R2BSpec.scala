package com.nulabinc.r2b.core

import java.util.Date

import com.nulabinc.backlog4j.{Issue => BacklogIssue}
import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.r2b.conf.AppConfiguration
import com.nulabinc.r2b.helper.SimpleFixture
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import com.taskadapter.redmineapi.bean.{Journal, User}
import com.taskadapter.redmineapi.bean.{Issue => RedmineIssue}
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class R2BSpec extends FlatSpec with Matchers with SimpleFixture {

  testProject(appConfiguration)
  testGroup(appConfiguration)
  testProjectUsers(appConfiguration)
  testVersion(appConfiguration)
  testTracker(appConfiguration)
  testWikis(appConfiguration)
  testIssues(appConfiguration)

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

  private[this] def testGroup(appConfiguration: AppConfiguration): Unit = {
    "Group" should "match" in {
      val backlogGroups = backlog.getGroups.asScala
      val memberShips   = redmine.getMembershipManager.getMemberships(appConfiguration.redmineConfig.projectKey).asScala

      val redmineGroups = memberShips.filter(_.getGroup != null).map(_.getGroup)
      redmineGroups.foreach(redmineGroup => {
        backlogGroups
          .filter(backlogGroup => backlogGroup.getName == redmineGroup.getName)
          .foreach(backlogGroup => {
            backlogGroup.getName should equal(redmineGroup.getName)
          })
      })

      val redmineUsers = memberShips
        .filter(_.getUser != null)
        .filter(_.getUser.getGroups != null)
        .map(memberShip => {
          redmine.getUserManager.getUserById(memberShip.getUser.getId)
        })
      redmineUsers.foreach(redmineUser => {
        redmineUser.getGroups.asScala.foreach(redmineGroup => {
          val optBacklogGroup = backlogGroups.find(backlogGroup => backlogGroup.getName == redmineGroup.getName)
          optBacklogGroup.isDefined should be(true)

          for { backlogGroup <- optBacklogGroup } yield {
            val backlogUsers = backlogGroup.getMembers.asScala
            backlogUsers.exists(backlogUser => {
              backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
            }) should be(true)
          }

        })
      })
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
          backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
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
    val backlogWikis = backlog.getWikis(appConfiguration.backlogConfig.projectKey).asScala.map(backlogWiki => backlog.getWiki(backlogWiki.getId))
    val redmineWikis = redmine.getWikiManager.getWikiPagesByProject(appConfiguration.redmineConfig.projectKey).asScala
    redmineWikis.foreach(redmineWiki =>
      "Wiki" should s"match: ${redmineWiki.getTitle}" in {
        val redmineWikiPageDetail =
          redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(appConfiguration.redmineConfig.projectKey, redmineWiki.getTitle)
        val backlogWiki = backlogWikis.find(redmineWikiPageDetail.getTitle == _.getName).get

        val sb = new StringBuilder
        if (redmineWikiPageDetail.getText != null) sb.append(redmineWikiPageDetail.getText)
        if (redmineWikiPageDetail.getComments != null)
          sb.append("\n\n\n").append(Messages("common.comment")).append(":").append(redmineWikiPageDetail.getComments)
        if (redmineWikiPageDetail.getParent != null)
          sb.append("\n").append(Messages("common.parent_page")).append(":[[").append(redmineWikiPageDetail.getParent.getTitle).append("]]")
        val redmineContent: String = sb.result()

        val redmineWikiUser = redmine.getUserManager.getUserById(redmineWikiPageDetail.getUser.getId)

        redmineWikiPageDetail.getTitle should equal(backlogWiki.getName)
        redmineContent should equal(backlogWiki.getContent)

        withClue(s"login:${redmineWikiUser.getLogin} converted:${userMapping.convert(redmineWikiUser.getLogin)}") {
          userMapping.convert(redmineWikiUser.getLogin) should equal(backlogWiki.getCreatedUser.getUserId)
        }
        withClue(s"login:${redmineWikiUser.getLogin} converted:${userMapping.convert(redmineWikiUser.getLogin)}") {
          userMapping.convert(redmineWikiUser.getLogin) should equal(backlogWiki.getUpdatedUser.getUserId)
        }
        timestampToString(redmineWikiPageDetail.getCreatedOn) should equal(timestampToString(backlogWiki.getCreated))

        redmineWikiPageDetail.getAttachments.asScala.foreach(redmineAttachment => {
          backlogWiki.getAttachments.asScala.exists(backlogAttachment => {
            backlogAttachment.getName == redmineAttachment.getFileName
          }) should be(true)
        })
    })
  }

  private[this] def testIssues(appConfiguration: AppConfiguration) = {
    val allCount = redmineIssueCount()
    val COUNT    = 100

    (0 until (allCount, COUNT)).foreach(offset => issues(COUNT, offset))
  }

  private[this] def issues(count: Int, offset: Long) = {
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
          dateToString(redmineIssue.getDueDate) should equal(dateToString(backlogIssue.getDueDate))

          //priority
          priorityMapping.convert(redmineIssue.getPriorityText) should equal(backlogIssue.getPriority.getName)

          //status
          withClue(s"""
               |status:${redmineIssue.getStatusName}
               |converted:${statusMapping.convert(redmineIssue.getStatusName)}
               |""".stripMargin) {
            statusMapping.convert(redmineIssue.getStatusName) should equal(backlogIssue.getStatus.getName)
          }

          //assignee
          if (redmineIssue.getAssignee != null) {
            val redmineUser = redmine.getUserManager.getUserById(redmineIssue.getAssignee.getId)
            userMapping.convert(redmineUser.getLogin) should equal(backlogIssue.getAssignee.getUserId)
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
            timestampToString(redmineIssue.getUpdatedOn) should be(timestampToString(updated(backlogIssue)))
          }

        }

    })
  }

  private[this] def updated(issue: BacklogIssue): Date = {
    val comments = backlog.getIssueComments(issue.getId)
    if (comments.isEmpty) issue.getUpdated
    else {
      val comment = comments.asScala.sortWith((c1, c2) => {
        val dt1 = new DateTime(c1.getUpdated)
        val dt2 = new DateTime(c2.getUpdated)
        dt1.isBefore(dt2)
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
    userMapping.convert(userOfId(user.getId.intValue()).getLogin)
  }

}

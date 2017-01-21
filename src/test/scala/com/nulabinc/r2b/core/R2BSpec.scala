package com.nulabinc.r2b.core

import com.nulabinc.backlog4j.api.option.GetIssuesParams
import com.nulabinc.r2b.actor.utils.IssueTag
import com.nulabinc.r2b.helper.SimpleFixture
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.Include
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class R2BSpec extends FlatSpec with Matchers with SimpleFixture {

  "Project" should "match" in {
    val redmineProject = redmine.getProjectManager.getProjectByKey(config.projectKeyMap.redmine)
    val backlogProject = backlog.getProject(config.projectKeyMap.getBacklogKey())

    backlogProject.getName should equal(redmineProject.getName)
    backlogProject.isChartEnabled should be(true)
    backlogProject.isSubtaskingEnabled should be(true)
    backlogProject.getTextFormattingRule should equal(com.nulabinc.backlog4j.Project.TextFormattingRule.Markdown)
  }

  "Group" should "match" in {
    val backlogGroups = backlog.getGroups.asScala
    val memberShips = redmine.getMembershipManager.getMemberships(config.projectKeyMap.redmine).asScala

    val redmineGroups = memberShips.filter(_.getGroup != null).map(_.getGroup)
    redmineGroups.foreach(redmineGroup => {
      backlogGroups.filter(backlogGroup => backlogGroup.getName == redmineGroup.getName).foreach(backlogGroup => {
        backlogGroup.getName should equal(redmineGroup.getName)
      })
    })

    val redmineUsers = memberShips.filter(_.getUser != null).filter(_.getUser.getGroups != null).map(memberShip => {
      redmine.getUserManager.getUserById(memberShip.getUser.getId)
    })
    redmineUsers.foreach(redmineUser => {
      redmineUser.getGroups.asScala.foreach(redmineGroup => {
        val optBacklogGroup = backlogGroups.find(backlogGroup => backlogGroup.getName == redmineGroup.getName)
        optBacklogGroup.isDefined should be(true)

        for {backlogGroup <- optBacklogGroup} yield {
          val backlogUsers = backlogGroup.getMembers.asScala
          backlogUsers.exists(backlogUser => {
            backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
          }) should be(true)
        }

      })
    })
  }

  "Project user" should "match" in {
    val backlogUsers = backlog.getProjectUsers(config.projectKeyMap.getBacklogKey()).asScala
    val memberShips = redmine.getMembershipManager.getMemberships(config.projectKeyMap.redmine).asScala

    val redmineUsers = memberShips.filter(_.getUser != null).map(memberShip => {
      redmine.getUserManager.getUserById(memberShip.getUser.getId)
    })

    redmineUsers.foreach(redmineUser => {
      backlogUsers.exists(backlogUser => {
        backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
      }) should be(true)
    })
  }

  "Version" should "match" in {
    val backlogVersions = backlog.getVersions(config.projectKeyMap.getBacklogKey()).asScala
    val redmineProject = redmine.getProjectManager.getProjectByKey(config.projectKeyMap.redmine)
    val redmineVersions = redmine.getProjectManager.getVersions(redmineProject.getId).asScala
    redmineVersions.foreach(redmineVersion => {
      val optBacklogVersion = backlogVersions.find(backlogVersion => redmineVersion.getName == backlogVersion.getName)
      optBacklogVersion.isDefined should be(true)
      for {backlogVersion <- optBacklogVersion} yield {
        redmineVersion.getName should equal(backlogVersion.getName)
        Option(redmineVersion.getDescription).getOrElse("") should equal(Option(backlogVersion.getDescription).getOrElse(""))
        dateToString(redmineVersion.getDueDate) should equal(dateToString(backlogVersion.getReleaseDueDate))
      }
    })
  }

  "Tracker" should "match" in {
    val backlogIssueTypes = backlog.getIssueTypes(config.projectKeyMap.getBacklogKey()).asScala
    val redmineTrackers = redmine.getIssueManager.getTrackers.asScala
    redmineTrackers.foreach(redmineTracker => {
      val backlogIssueType = backlogIssueTypes.find(backlogIssueType => redmineTracker.getName == backlogIssueType.getName).get
      redmineTracker.getName should equal(backlogIssueType.getName)
    })
  }

  val backlogWikis = backlog.getWikis(config.projectKeyMap.getBacklogKey()).asScala.map(backlogWiki => backlog.getWiki(backlogWiki.getId))
  val redmineWikis = redmine.getWikiManager.getWikiPagesByProject(config.projectKeyMap.redmine).asScala
  redmineWikis.foreach(redmineWiki => "Wiki" should s"match: ${redmineWiki.getTitle}" in {
    val redmineWikiPageDetail = redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(config.projectKeyMap.redmine, redmineWiki.getTitle)
    val backlogWiki = backlogWikis.find(redmineWikiPageDetail.getTitle == _.getName).get

    val sb = new StringBuilder
    if (redmineWikiPageDetail.getText != null) sb.append(redmineWikiPageDetail.getText)
    if (redmineWikiPageDetail.getComments != null) sb.append("\n\n\n").append(Messages("common.comment")).append(":").append(redmineWikiPageDetail.getComments)
    if (redmineWikiPageDetail.getParent != null) sb.append("\n").append(Messages("common.parent_page")).append(":[[").append(redmineWikiPageDetail.getParent.getTitle).append("]]")
    val redmineContent: String = sb.result()

    val redmineWikiUser = redmine.getUserManager.getUserById(redmineWikiPageDetail.getUser.getId)
    val redmineWikiUserId = userMapping.convert(redmineWikiUser.getLogin)

    redmineWikiPageDetail.getTitle should equal(backlogWiki.getName)
    redmineContent should equal(backlogWiki.getContent)
    redmineWikiUserId should equal(backlogWiki.getCreatedUser.getUserId)
    redmineWikiUserId should equal(backlogWiki.getUpdatedUser.getUserId)
    timestampToString(redmineWikiPageDetail.getCreatedOn) should equal(timestampToString(backlogWiki.getCreated))

    redmineWikiPageDetail.getAttachments.asScala.foreach(redmineAttachment => {
      backlogWiki.getAttachments.asScala.exists(backlogAttachment => {
        backlogAttachment.getName == redmineAttachment.getFileName
      }) should be(true)
    })
  })


  val allCount = redmineIssueCount()
  val COUNT = 100

  def loop(offset: Long): Unit = {
    if (offset < allCount) {
      issues(COUNT, offset)
      loop(offset + COUNT)
    }
  }

  loop(0)

  private[this] def issues(count: Int, offset: Long) = {
    val backlogProject = backlog.getProject(config.projectKeyMap.getBacklogKey())
    val params = new GetIssuesParams(List(java.lang.Long.valueOf(backlogProject.getId)).asJava)
    val backlogIssues = backlog.getIssues(params).asScala

    val redmineIssues = getRedmineIssues(count, offset).map(redmineIssue => {
      redmine.getIssueManager.getIssueById(redmineIssue.getId, Include.attachments, Include.journals)
    })

    redmineIssues.foreach(redmineIssue => "Issue" should s"match: ${redmineIssue.getId}" in {
      val backlogIssue = backlogIssues.find(backlogIssue => redmineIssue.getSubject == backlogIssue.getSummary).get
      val redmineDescription = new StringBuilder
      redmineDescription.append(redmineIssue.getDescription)
      redmineDescription.append("\n").append(Messages("common.done_ratio")).append(":").append(redmineIssue.getDoneRatio)
      redmineDescription.append("\n").append(IssueTag.getTag(redmineIssue.getId, config.redmineConfig.url))

      val spentHours = BigDecimal(redmineIssue.getSpentHours).setScale(2, BigDecimal.RoundingMode.HALF_UP)
      val actualHours = BigDecimal(backlogIssue.getActualHours).setScale(2, BigDecimal.RoundingMode.HALF_UP)

      val redmineIssueUser = redmine.getUserManager.getUserById(redmineIssue.getAuthor.getId)
      val redmineIssueUserId = userMapping.convert(redmineIssueUser.getLogin)

      //description
      //redmineDescription.result() should equal(backlogIssue.getDescription)

      //issue type
      redmineIssue.getTracker.getName should equal(backlogIssue.getIssueType.getName)

      //category
      if (redmineIssue.getCategory != null) {
        redmineIssue.getCategory.getName should equal(backlogIssue.getCategory.get(0).getName)
      }

      //milestone
      if (redmineIssue.getTargetVersion != null) {
        redmineIssue.getTargetVersion.getName should equal(backlogIssue.getMilestone.get(0).getName)
      }

      //due date
      dateToString(redmineIssue.getDueDate) should equal(dateToString(backlogIssue.getDueDate))

      //priority
      priorityMapping.convert(redmineIssue.getPriorityText) should equal(backlogIssue.getPriority.getName)

      //status
      statusMapping.convert(redmineIssue.getStatusName) should equal(backlogIssue.getStatus.getName)

      //assignee
      if (redmineIssue.getAssignee != null) {
        val redmineUser = redmine.getUserManager.getUserById(redmineIssue.getAssignee.getId)
        userMapping.convert(redmineUser.getLogin) should equal(backlogIssue.getAssignee.getUserId)
      }

      //actual hours
      spentHours should equal(actualHours)

      //start date
      dateToString(redmineIssue.getStartDate) should equal(dateToString(backlogIssue.getStartDate))

      //created user
      redmineIssueUserId should equal(backlogIssue.getCreatedUser.getUserId)

      //updated user
      redmineIssueUserId should equal(backlogIssue.getUpdatedUser.getUserId)

      //created
      timestampToString(redmineIssue.getCreatedOn) should equal(timestampToString(backlogIssue.getCreated))

    })
  }

}

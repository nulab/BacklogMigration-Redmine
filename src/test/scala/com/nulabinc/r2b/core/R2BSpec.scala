package com.nulabinc.r2b.core

import com.nulabinc.backlog.importer.actor.backlog.BacklogActor
import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.BacklogClient
import com.nulabinc.r2b.actor.convert.ConvertActor
import com.nulabinc.r2b.actor.redmine.RedmineActor
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.helper.SimpleFixture
import com.osinka.i18n.Messages
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.collection.JavaConversions._
import scalax.file.Path

/**
  * @author uchida
  */
class R2BSpec extends FlatSpec with ShouldMatchers with BeforeAndAfterAll with SimpleFixture {

  override def beforeAll(): Unit = {
    val r2bRoot: Path = Path.fromString(ConfigBase.R2B_ROOT)
    r2bRoot.deleteRecursively(force = true, continueOnFailure = true)

    val conf: R2BConfig = getConfig
    RedmineActor(conf)
    ConvertActor(conf)
    BacklogActor(BacklogConfig(conf.backlogUrl, conf.backlogKey))
  }

  override def afterAll(): Unit = {
    val conf: R2BConfig = getConfig
    val backlog: BacklogClient = getBacklogClient(conf)
    backlog.deleteProject(conf.projects(0).getBacklogKey())
  }

  "Project" should "match" in {
    val redmineProject = redmine.getProjectManager.getProjectByKey(conf.projects(0).redmine)
    val backlogProject = backlog.getProject(conf.projects(0).getBacklogKey())

    backlogProject.getName should equal(redmineProject.getName)
    backlogProject.isChartEnabled should be(true)
    backlogProject.isSubtaskingEnabled should be(true)
    backlogProject.getTextFormattingRule should equal(com.nulabinc.backlog4j.Project.TextFormattingRule.Markdown)
  }

  "Group" should "match" in {
    val backlogGroups = backlog.getGroups
    val memberShips = redmine.getMembershipManager.getMemberships(conf.projects(0).redmine)

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
      redmineUser.getGroups.foreach(redmineGroup => {
        val targetGroup = backlogGroups.find(backlogGroup => backlogGroup.getName == redmineGroup.getName).get
        val backlogUsers = targetGroup.getMembers
        backlogUsers.exists(backlogUser => {
          backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
        }) should be(true)
      })
    })
  }

  "Project user" should "match" in {
    val backlogUsers = backlog.getProjectUsers(conf.projects(0).getBacklogKey())
    val memberShips = redmine.getMembershipManager.getMemberships(conf.projects(0).redmine)

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
    val backlogVersions = backlog.getVersions(conf.projects(0).getBacklogKey())
    val redmineProject = redmine.getProjectManager.getProjectByKey(conf.projects(0).redmine)
    val redmineVersions = redmine.getProjectManager.getVersions(redmineProject.getId)
    redmineVersions.foreach(redmineVersion => {
      val backlogVersion = backlogVersions.find(backlogVersion => redmineVersion.getName == backlogVersion.getName).get
      redmineVersion.getName should equal(backlogVersion.getName)
      redmineVersion.getDescription should equal(backlogVersion.getDescription)
      dateToString(redmineVersion.getDueDate) should equal(dateToString(backlogVersion.getReleaseDueDate))
    })
  }

  "Tracker" should "match" in {
    val backlogIssueTypes = backlog.getIssueTypes(conf.projects(0).getBacklogKey())
    val redmineTrackers = redmine.getIssueManager.getTrackers
    redmineTrackers.foreach(redmineTracker => {
      val backlogIssueType = backlogIssueTypes.find(backlogIssueType => redmineTracker.getName == backlogIssueType.getName).get
      redmineTracker.getName should equal(backlogIssueType.getName)
    })
  }

  "Wiki" should "match" in {
    val backlogWikis = backlog.getWikis(conf.projects(0).getBacklogKey()).map(backlogWiki => backlog.getWiki(backlogWiki.getId))
    val redmineWikis = redmine.getWikiManager.getWikiPagesByProject(conf.projects(0).redmine)
    redmineWikis.foreach(redmineWiki => {
      val redmineWikiPageDetail = redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(conf.projects(0).redmine, redmineWiki.getTitle)
      val backlogWiki = backlogWikis.find(redmineWikiPageDetail.getTitle == _.getName).get

      val sb = new StringBuilder
      if (redmineWikiPageDetail.getText != null) sb.append(redmineWikiPageDetail.getText)
      if (redmineWikiPageDetail.getComments != null) sb.append("\n\n\n").append(Messages("label.comment")).append(":").append(redmineWikiPageDetail.getComments)
      if (redmineWikiPageDetail.getParent != null) sb.append("\n").append(Messages("label.parent_page")).append(":[[").append(redmineWikiPageDetail.getParent.getTitle).append("]]")
      val redmineContent: String = sb.result()

      val redmineWikiUser = redmine.getUserManager.getUserById(redmineWikiPageDetail.getUser.getId)
      val redmineWikiUserId = userMapping.convert(redmineWikiUser.getLogin)

      redmineWikiPageDetail.getTitle should equal(backlogWiki.getName)
      redmineContent.replaceAll("\r\n","\n") should equal(backlogWiki.getContent)
      redmineWikiUserId should equal(backlogWiki.getCreatedUser.getUserId)
      redmineWikiUserId should equal(backlogWiki.getUpdatedUser.getUserId)
      timestampToString(redmineWikiPageDetail.getCreatedOn) should equal(timestampToString(backlogWiki.getCreated))

      redmineWikiPageDetail.getAttachments.foreach(redmineAttachment => {
        backlogWiki.getAttachments.exists(backlogAttachment => {
          backlogAttachment.getName == redmineAttachment.getFileName
        }) should be(true)
      })
    })

  }


}

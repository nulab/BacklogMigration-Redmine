package com.nulabinc.r2b.core

import com.nulabinc.backlog.importer.actor.backlog.BacklogActor
import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.{BacklogClient, Group => BacklogGroup, Project => BacklogProject, User => BacklogUser}
import com.nulabinc.r2b.actor.convert.ConvertActor
import com.nulabinc.r2b.actor.redmine.RedmineActor
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.helper.SimpleFixture
import com.taskadapter.redmineapi.bean.{Group => RedmineGroup, Membership, Project => RedmineProject, User => RedmineUser}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.collection.JavaConversions._

/**
  * @author uchida
  */
class R2BSpec extends FlatSpec with ShouldMatchers with BeforeAndAfterAll with SimpleFixture {

  override def beforeAll(): Unit = {
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
    val redmineProject: RedmineProject = redmine.getProjectManager.getProjectByKey(conf.projects(0).redmine)
    val backlogProject: BacklogProject = backlog.getProject(conf.projects(0).getBacklogKey())

    backlogProject.getName should equal(redmineProject.getName)
    backlogProject.isChartEnabled should be(true)
    backlogProject.isSubtaskingEnabled should be(true)
    backlogProject.getTextFormattingRule should equal(BacklogProject.TextFormattingRule.Markdown)
  }

  "Group" should "match" in {
    val backlogGroups: Seq[BacklogGroup] = backlog.getGroups
    val memberShips: Seq[Membership] = redmine.getMembershipManager.getMemberships(conf.projects(0).redmine)

    val redmineGroups: Seq[RedmineGroup] = memberShips.filter(_.getGroup != null).map(_.getGroup)
    redmineGroups.foreach(redmineGroup => {
      backlogGroups.filter(backlogGroup => backlogGroup.getName == redmineGroup.getName).foreach(backlogGroup => {
        backlogGroup.getName should equal(redmineGroup.getName)
      })
    })

    val redmineUsers: Seq[RedmineUser] = memberShips.filter(_.getUser != null).filter(_.getUser.getGroups != null).map(memberShip => {
      redmine.getUserManager.getUserById(memberShip.getUser.getId)
    })
    redmineUsers.foreach(redmineUser => {
      redmineUser.getGroups.foreach(redmineGroup => {
        val targetGroup: BacklogGroup = backlogGroups.find(backlogGroup => backlogGroup.getName == redmineGroup.getName).get
        val backlogUsers: Seq[BacklogUser] = targetGroup.getMembers
        backlogUsers.exists(backlogUser => {
          backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
        }) should be(true)
      })
    })
  }

  "Project user" should "match" in {
    val backlogUsers: Seq[BacklogUser] = backlog.getProjectUsers(conf.projects(0).getBacklogKey())
    val memberShips: Seq[Membership] = redmine.getMembershipManager.getMemberships(conf.projects(0).redmine)

    val redmineUsers: Seq[RedmineUser] = memberShips.filter(_.getUser != null).map(memberShip => {
      redmine.getUserManager.getUserById(memberShip.getUser.getId)
    })

    redmineUsers.foreach(redmineUser => {
      backlogUsers.exists(backlogUser => {
        backlogUser.getUserId == userMapping.convert(redmineUser.getLogin)
      }) should be(true)
    })
  }


}

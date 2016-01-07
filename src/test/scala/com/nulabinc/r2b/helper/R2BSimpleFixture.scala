package com.nulabinc.r2b.helper

import java.io.FileInputStream
import java.util.Properties

import com.nulabinc.backlog4j.conf.{BacklogConfigure, BacklogPackageConfigure}
import com.nulabinc.backlog4j.{BacklogClient, BacklogClientFactory, Project => BacklogProject}
import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.R2BConfig
import com.taskadapter.redmineapi.bean.{Project => RedmineProject}
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import org.specs2.specification.Scope

/**
  * @author uchida
  */
trait R2BSimpleFixture extends Scope {

  val conf: R2BConfig = getConfig
  val redmine: RedmineManager = RedmineManagerFactory.createWithApiKey(conf.redmineUrl, conf.redmineKey)
  val backlog: BacklogClient = getBacklogClient(conf)

  def getConfig: R2BConfig = {
    val prop: Properties = new Properties()
    prop.load(new FileInputStream("app.properties"))
    val backlogKey: String = prop.getProperty("backlog.key")
    val backlogUrl: String = prop.getProperty("backlog.url")
    val redmineKey: String = prop.getProperty("redmine.key")
    val redmineUrl: String = prop.getProperty("redmine.url")
    val project: String = prop.getProperty("project")

    val paramProjectKey: ParamProjectKey = new ParamProjectKey(project)

    val conf: R2BConfig = new R2BConfig(
      backlogUrl, backlogKey,
      redmineUrl, redmineKey,
      List(paramProjectKey))
    conf
  }

  def getBacklogClient(conf: R2BConfig): BacklogClient = {
    val url = conf.backlogUrl
    val backlogPackageConfigure: BacklogPackageConfigure = new BacklogPackageConfigure(url)
    val configure: BacklogConfigure = backlogPackageConfigure.apiKey(conf.backlogKey)
    new BacklogClientFactory(configure).newClient()
  }

}

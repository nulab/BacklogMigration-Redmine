package com.nulabinc.backlog.r2b.conf

import com.nulabinc.backlog.migration.common.conf.BacklogApiConfiguration
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration

case class ExcludeOption(issue: Boolean, wiki: Boolean) {
  override def toString: String = {
    val issueArr = if (issue) Seq("issue") else Seq()
    val wikiArr = if (wiki) Seq("wiki") else Seq()

    (issueArr ++ wikiArr).mkString(", ")
  }
}

object ExcludeOption {
  val default: ExcludeOption = ExcludeOption(issue = false, wiki = false)
}

case class AppConfiguration(
  redmineConfig: RedmineApiConfiguration,
  backlogConfig: BacklogApiConfiguration,
  exclude: ExcludeOption,
  importOnly: Boolean,
  retryCount: Int
)

case class DestroyConfiguration(
  backlogConfig: BacklogApiConfiguration,
  dryRun: Boolean
)
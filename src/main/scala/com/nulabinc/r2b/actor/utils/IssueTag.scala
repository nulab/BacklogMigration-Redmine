package com.nulabinc.r2b.actor.utils

/**
 * @author uchida
 */
object IssueTag {

  //Backlog
  //Ref: From Redmine [[#3>http://ec2-52-69-114-228.ap-northeast-1.compute.amazonaws.com:10083/issues/3]]
  //Markdown
  //Ref: From Redmine [#3](http://ec2-52-69-114-228.ap-northeast-1.compute.amazonaws.com:10083/issues/3)
  //s"[REDMINE#$issueId]"
  def getTag(issueId: Int, url: String): String =
    s"Ref: From Redmine [#$issueId]($url/issues/$issueId)"
    //s"Ref: From Redmine [[#$issueId>$url/issues/$issueId]]"

  def isTaged(issueId: Int, text: String, url: String): Boolean = text.contains(getTag(issueId, url))

}

package com.nulabinc.r2b.actor.redmine

import com.taskadapter.redmineapi.bean.User

/**
  * @author uchida
  */
case class ExportInfo(needUsers: Seq[User])
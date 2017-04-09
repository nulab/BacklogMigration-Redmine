package com.nulabinc.r2b.redmine.domain

import com.taskadapter.redmineapi.bean.{User, Version}

/**
  * @author uchida
  */
case class PropertyValue(versions: Seq[Version], users: Seq[User])

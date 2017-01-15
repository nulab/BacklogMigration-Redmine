package com.nulabinc.r2b.mapping

import com.taskadapter.redmineapi.bean.User

import scala.collection.mutable

/**
  * @author uchida
  */
case class MappingData(users: mutable.Set[User], statuses: mutable.Set[String])

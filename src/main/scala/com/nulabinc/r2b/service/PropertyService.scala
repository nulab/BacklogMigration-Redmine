package com.nulabinc.r2b.service

/**
  * @author uchida
  */
trait PropertyService {

  def reload()

  def allVersionNames(): Seq[String]

  def allMembershipNames(): Seq[String]

  def optUser(id: Int): Option[String]

  def optUser(optId: Option[String]): Option[String]

  def optUserName(optId: Option[String]): Option[String]

  def optVersionName(optId: Option[String]): Option[String]

  def optMembershipName(optId: Option[String]): Option[String]

  def optStatusName(optId: Option[String]): Option[String]

  def optTrackerName(optId: Option[String]): Option[String]

  def optPriorityName(optId: Option[String]): Option[String]

  def optCategoryName(optId: Option[String]): Option[String]

  def optDefaultStatusName(): Option[String]

  def optProjectName(id: Int): Option[String]

}

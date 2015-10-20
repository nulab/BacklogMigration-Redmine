package com.nulabinc.r2b.conf

import com.nulabinc.r2b.cli.ParamProjectKey

/**
 * @author uchida
 */
case class R2BConfig(
                      backlogUrl: String,
                      backlogKey: String,
                      redmineUrl: String,
                      redmineKey: String,
                      projects: Seq[ParamProjectKey])
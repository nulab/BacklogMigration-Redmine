package com.nulabinc.backlog.r2b.utils
import com.nulabinc.backlog.migration.common.domain.BacklogTextFormattingRule

object TextileUtil {

  def convert(value: String, backlogTextFormattingRule: BacklogTextFormattingRule) = {
    if (Option(value).isDefined) {
      if (backlogTextFormattingRule.value == "backlog") {
        value.replace("<pre>", "{code}").replace("</pre>", "{/code}")
      } else {
        value.replace("<pre>", "```").replace("</pre>", "```")
      }
    } else value
  }

}

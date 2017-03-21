package com.nulabinc.r2b.service.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.conf.{RedmineDirectory, RedmineProperty}
import com.nulabinc.r2b.domain.{RedmineCustomFieldDefinition, RedmineJournalDetail}
import com.nulabinc.r2b.service._
import com.osinka.i18n.Messages

/**
  * @author uchida
  */
class ConvertJournalDetailServiceImpl @Inject()(
                                                 redmineDirectory: RedmineDirectory,
                                                 propertyService: PropertyService,
                                                 customFieldService: CustomFieldService) extends ConvertJournalDetailService with Logging {

  private[this] val customFieldDefinitions = customFieldService.allCustomFieldDefinitions()

  override def needNote(detail: RedmineJournalDetail, issueId: Int): Boolean =
    RelationNote.is(detail, issueId) ||
      DoneRatioNote.is(detail, issueId) ||
      PrivateIssueNote.is(detail, issueId) ||
      ProjectIdNote.is(detail, issueId) ||
      AnonymousUserNote.is(detail, issueId) ||
      AttachmentNotFoundNote.is(detail, issueId)

  override def getValue(detail: RedmineJournalDetail, issueId: Int): String =
    if (DoneRatioNote.is(detail, issueId)) DoneRatioNote.value(detail)
    else if (PrivateIssueNote.is(detail, issueId)) PrivateIssueNote.value(detail)
    else if (RelationNote.is(detail, issueId)) RelationNote.value(detail)
    else if (ProjectIdNote.is(detail, issueId)) ProjectIdNote.value(detail)
    else if (AnonymousUserNote.is(detail, issueId)) AnonymousUserNote.value(detail)
    else if (AttachmentNotFoundNote.is(detail, issueId)) AttachmentNotFoundNote.value(detail)
    else ""

  trait Note {

    def is(detail: RedmineJournalDetail, issueId: Int): Boolean

    def value(detail: RedmineJournalDetail): String

    def getNote(label: String, detail: RedmineJournalDetail): String =
      Messages(label, getValue(detail.oldValue), getValue(detail.newValue))

    def getMessage(label: String, oldValue: String, newValue: String): String =
      Messages("common.change_comment", Messages(label), oldValue, newValue)

    def getValue(value: Option[String]): String = value.getOrElse(Messages("common.not_set"))

  }

  object RelationNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean = detail.property == RedmineProperty.RELATION

    override def value(detail: RedmineJournalDetail): String = getNote("common.relation", detail)
  }

  object DoneRatioNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean =
      detail.property == RedmineProperty.ATTR && detail.name == "done_ratio"

    override def value(detail: RedmineJournalDetail): String = getNote("common.done_ratio", detail)
  }

  object PrivateIssueNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean =
      detail.property == RedmineProperty.ATTR && detail.name == "is_private"

    override def value(detail: RedmineJournalDetail): String = getNote("common.private", detail)
  }

  object ProjectIdNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean =
      detail.property == RedmineProperty.ATTR && detail.name == "project_id"

    override def value(detail: RedmineJournalDetail): String = {
      val oldName = getProjectName(detail.oldValue)
      val newName = getProjectName(detail.newValue)
      getMessage("common.project", oldName, newName)
    }

    private[this] def getProjectName(optProjectId: Option[String]): String =
      optProjectId match {
        case Some(projectId) if (projectId.nonEmpty) =>
          propertyService.optProjectName(projectId.toInt).getOrElse(Messages("common.not_set"))
        case _ => Messages("common.not_set")
      }
  }

  object AnonymousUserNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean = {
      if (detail.property == RedmineProperty.CUSTOM_FIELD) {
        optCustomFieldDefinition(detail.name.toInt) match {
          case Some(customFieldDefinition) =>
            if (customFieldDefinition.fieldFormat == "user")
              !(propertyService.optUserName(detail.oldValue).isDefined && propertyService.optUserName(detail.newValue).isDefined)
            else false
          case _ => false
        }
      } else false
    }

    override def value(detail: RedmineJournalDetail): String = {
      val oldValue = propertyService.optUserName(detail.oldValue).orElse(detail.oldValue)
      val newValue = propertyService.optUserName(detail.newValue).orElse(detail.newValue)
      getMessage("common.user", getValue(oldValue), getValue(newValue))
    }
  }

  object AttachmentNotFoundNote extends Note {
    override def is(detail: RedmineJournalDetail, issueId: Int): Boolean = {
      if (detail.property == RedmineProperty.ATTACHMENT) {
        val dir: String = redmineDirectory.getIssueAttachmentDir(issueId, detail.name.toInt)
        !IOUtil.isDirectory(dir)
      } else false
    }

    override def value(detail: RedmineJournalDetail): String =
      (detail.newValue, detail.oldValue) match {
        case (Some(newValue), _) => Messages("convert.add_attachment", newValue)
        case (_, Some(oldValue)) => Messages("message.del_attachment", oldValue)
        case _ => ""
      }
  }

  private[this] def optCustomFieldDefinition(id: Int): Option[RedmineCustomFieldDefinition] =
    customFieldDefinitions.find(_.id == id)

}

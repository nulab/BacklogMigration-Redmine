package com.nulabinc.r2b.service

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.channels.{Channels, ReadableByteChannel}
import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.{IOUtil, Logging}
import com.nulabinc.r2b.conf.RedmineDirectory
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
class AttachmentDownloadServiceImpl @Inject()(redmineDirectory: RedmineDirectory) extends AttachmentDownloadService with Logging {

  private[this] case class DownloadInfo(url: URL, path: String)

  override def issue(apiKey: String, issue: Issue) = {
    val attachments: Array[Attachment] = issue.getAttachments.toArray(new Array[Attachment](issue.getAttachments.size()))
    getDownloadInfos(apiKey, issue)(attachments).foreach(download)
  }

  override def wiki(apiKey: String, wikiPageDetail: WikiPageDetail) = {
    val attachments: Array[Attachment] = wikiPageDetail.getAttachments.toArray(new Array[Attachment](wikiPageDetail.getAttachments.size()))
    getDownloadInfos(apiKey, wikiPageDetail)(attachments).foreach(download)
  }

  private[this] def getDownloadInfos(apiKey: String, issue: Issue)(attachments: Array[Attachment]) =
    attachments.map(attachment => getDownloadInfo(apiKey, issue, attachment))

  private[this] def getDownloadInfos(apiKey: String, wikiPageDetail: WikiPageDetail)(attachments: Array[Attachment]) =
    attachments.map(attachment => getDownloadInfo(apiKey, wikiPageDetail, attachment))

  private[this] def getDownloadInfo(apiKey: String, issue: Issue, attachment: Attachment): DownloadInfo = {
    val directoryPath: String = redmineDirectory.getIssueAttachmentDir(issue.getId, attachment.getId)
    getDownloadInfo(apiKey, attachment, directoryPath)
  }

  private[this] def getDownloadInfo(apiKey: String, wikiPageDetail: WikiPageDetail, attachment: Attachment): DownloadInfo = {
    val directoryPath: String = redmineDirectory.getWikiAttachmentDir(wikiPageDetail.getTitle, attachment.getId)
    getDownloadInfo(apiKey, attachment, directoryPath)
  }

  private[this] def getDownloadInfo(apiKey: String, attachment: Attachment, directoryPath: String): DownloadInfo = {
    IOUtil.createDirectory(directoryPath)
    val url: URL = new URL(attachment.getContentURL + "?key=" + apiKey)
    val path: String = directoryPath + File.separator + attachment.getFileName
    DownloadInfo(url, path)
  }

  private[this] def download(downloadInfo: DownloadInfo) = {
    val rbc: ReadableByteChannel = Channels.newChannel(downloadInfo.url.openStream())
    val fos: FileOutputStream = new FileOutputStream(downloadInfo.path)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

    rbc.close()
    fos.close()
  }

}

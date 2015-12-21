package com.nulabinc.r2b.service

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.channels.{Channels, ReadableByteChannel}

import com.nulabinc.r2b.conf.ConfigBase
import com.nulabinc.r2b.utils.IOUtil
import com.taskadapter.redmineapi.bean._

/**
  * @author uchida
  */
object AttachmentDownloader {

  private case class DownloadInfo(url: URL, path: String)

  def issue(apiKey: String, projectIdentifier: String, issue: Issue) = {
    val attachments: Array[Attachment] = issue.getAttachments.toArray(new Array[Attachment](issue.getAttachments.size()))
    getDownloadInfos(apiKey, projectIdentifier, issue)(attachments).foreach(download)
  }

  def wiki(apiKey: String, projectIdentifier: String, wikiPageDetail: WikiPageDetail) = {
    val attachments: Array[Attachment] = wikiPageDetail.getAttachments.toArray(new Array[Attachment](wikiPageDetail.getAttachments.size()))
    getDownloadInfos(apiKey, projectIdentifier, wikiPageDetail)(attachments).foreach(download)
  }

  private def getDownloadInfos(apiKey: String, projectIdentifier: String, issue: Issue)(attachments: Array[Attachment]) =
    attachments.map(attachment => getDownloadInfo(apiKey, projectIdentifier, issue, attachment))

  private def getDownloadInfos(apiKey: String, projectIdentifier: String, wikiPageDetail: WikiPageDetail)(attachments: Array[Attachment]) =
    attachments.map(attachment => getDownloadInfo(apiKey, projectIdentifier, wikiPageDetail, attachment))

  private def getDownloadInfo(apiKey: String, projectIdentifier: String, issue: Issue, attachment: Attachment): DownloadInfo = {
    val directoryPath: String = ConfigBase.Redmine.getIssueAttachmentDir(projectIdentifier, issue.getId, attachment.getId)
    getDownloadInfo(apiKey, attachment, directoryPath)
  }

  private def getDownloadInfo(apiKey: String, projectIdentifier: String, wikiPageDetail: WikiPageDetail, attachment: Attachment): DownloadInfo = {
    val directoryPath: String = ConfigBase.Redmine.getWikiAttachmentDir(projectIdentifier, wikiPageDetail.getTitle, attachment.getId)
    getDownloadInfo(apiKey, attachment, directoryPath)
  }

  private def getDownloadInfo(apiKey: String, attachment: Attachment, directoryPath: String): DownloadInfo = {
    IOUtil.createDirectory(directoryPath)
    val url: URL = new URL(attachment.getContentURL + "?key=" + apiKey)
    val path: String = directoryPath + File.separator + attachment.getFileName
    DownloadInfo(url, path)
  }

  private def download(downloadInfo: DownloadInfo) = {
    val rbc: ReadableByteChannel = Channels.newChannel(downloadInfo.url.openStream())
    val fos: FileOutputStream = new FileOutputStream(downloadInfo.path)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
  }

}

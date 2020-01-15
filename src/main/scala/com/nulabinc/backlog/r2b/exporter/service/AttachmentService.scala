package com.nulabinc.backlog.r2b.exporter.service

import java.io.{File, FileOutputStream}
import java.net.{HttpURLConnection, URL}
import java.nio.channels.Channels

import com.nulabinc.backlog.migration.common.utils.ControlUtil.using
import com.nulabinc.backlog.migration.common.utils.Logging

object AttachmentService extends Logging {
  private val MAX_REDIRECT_COUNT = 10

  def download(url: URL, file: File): Unit = {
    val redirected = followRedirect(url)

    doDownload(redirected, file)
  }

  private def doDownload(url: URL, file: File): Unit =
    try {
      val rbc = Channels.newChannel(url.openStream())
      val fos = new FileOutputStream(file)
      fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)

      rbc.close()
      fos.close()
    } catch {
      case e: Throwable => logger.warn("Download attachment failed: " + e.getMessage)
    }

  private def followRedirect(url: URL, count: Int = 0): URL =
    url.openConnection match {
      case http: HttpURLConnection =>
        http.setRequestMethod("GET")
        http.connect()
        using(http) { connection =>
          connection.getResponseCode match {
            case 301 | 302 | 303 =>
              val newUrl = new URL(connection.getHeaderField("Location"))
              if (count < MAX_REDIRECT_COUNT) followRedirect(newUrl, count + 1) else newUrl
            case _ =>
              url
          }
        }
      case _ =>
        url
    }
}

package com.nulabinc.backlog.r2b.utils

import java.security.cert.X509Certificate
import javax.net.ssl._

import com.nulabinc.backlog.migration.common.utils.Logging

/**
 * @author
 *   uchida
 */
object DisableSSLCertificateCheckUtil extends Logging {

  def disableChecks(): Unit = {
    try {
      val context: SSLContext = SSLContext.getInstance("TLS")
      val trustManagerArray: Array[TrustManager] = Array(
        new NullX509TrustManager()
      )
      context.init(null, trustManagerArray, null)
      HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory)
      HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier())
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
    }
  }

  private[this] class NullX509TrustManager extends X509TrustManager {

    override def checkClientTrusted(
        chain: Array[X509Certificate],
        authType: String
    ): Unit = ()

    override def checkServerTrusted(
        chain: Array[X509Certificate],
        authType: String
    ): Unit = ()

    override def getAcceptedIssuers: Array[X509Certificate] =
      Array.ofDim[X509Certificate](0)
  }

  private[this] class NullHostnameVerifier extends HostnameVerifier {

    override def verify(hostname: String, session: SSLSession): Boolean = true
  }

}

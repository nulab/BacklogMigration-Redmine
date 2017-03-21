package com.nulabinc.r2b.utils

import java.security.cert.X509Certificate
import javax.net.ssl._

import com.nulabinc.backlog.migration.utils.Logging

/**
  * @author uchida
  */

object DisableSSLCertificateCheckUtil extends Logging {

  private[this] class NullX509TrustManager extends X509TrustManager {

    def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
    }

    def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
    }

    def getAcceptedIssuers: Array[X509Certificate] = Array.ofDim[X509Certificate](0)
  }

  private[this] class NullHostnameVerifier extends HostnameVerifier {

    def verify(hostname: String, session: SSLSession): Boolean = true
  }

  def disableChecks() {
    try {
      val context: SSLContext = SSLContext.getInstance("TLS")
      val trustManagerArray: Array[TrustManager] = Array(new NullX509TrustManager())
      context.init(null, trustManagerArray, null)
      HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory)
      HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier())
    } catch {
      case e: Exception =>
        log.error(e)
    }
  }
}

package com.nulabinc.r2b.utils

import java.security.cert.X509Certificate
import javax.net.ssl._

/**
  * @author uchida
  */

object DisableSSLCertificateCheckUtil {

  private class NullX509TrustManager extends X509TrustManager {

    def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
      println()
    }

    def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
      println()
    }

    def getAcceptedIssuers(): Array[X509Certificate] = Array.ofDim[X509Certificate](0)
  }

  private class NullHostnameVerifier extends HostnameVerifier {

    def verify(hostname: String, session: SSLSession): Boolean = true
  }

  def disableChecks() {
    try {
      var sslc: SSLContext = null
      sslc = SSLContext.getInstance("TLS")
      val trustManagerArray: Array[TrustManager] = Array(new NullX509TrustManager())
      sslc.init(null, trustManagerArray, null)
      HttpsURLConnection.setDefaultSSLSocketFactory(sslc.getSocketFactory)
      HttpsURLConnection.setDefaultHostnameVerifier(new NullHostnameVerifier())
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}

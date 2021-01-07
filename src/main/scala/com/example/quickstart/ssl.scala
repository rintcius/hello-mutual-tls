package com.example.quickstart

import cats.effect.Sync
import cats.syntax.all._
import java.security.{KeyStore, Security}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import org.http4s.HttpApp
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Host, Location}
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

object ssl {
  val keystorePassword: String = "secret"
  val keyManagerPassword: String = "secret"

  val combiPath = "/combi/client.jks"
  val combiPathP12 = "/combi/client.p12"

  def keyManagerFactory[F[_]](keystorePassword: String, keyManagerPass: String, idPath: String)(implicit
      F: Sync[F]): F[KeyManagerFactory] =
    F.delay {
      val ks = KeyStore.getInstance("JKS")

      val ksStream = this.getClass.getResourceAsStream(idPath)
      ks.load(ksStream, keystorePassword.toCharArray)
      ksStream.close()

      val kmf = KeyManagerFactory.getInstance(
        Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
          .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

      kmf.init(ks, keyManagerPass.toCharArray)

      kmf
    }

  def loadContextFromClasspath[F[_]](keystorePassword: String, keyManagerPass: String, idPath: String)(implicit
      F: Sync[F]): F[SSLContext] =
    F.delay {
      val ks = KeyStore.getInstance("JKS")

      val ksStream = this.getClass.getResourceAsStream(idPath)
      ks.load(ksStream, keystorePassword.toCharArray)
      ksStream.close()

      val kmf = KeyManagerFactory.getInstance(
        Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
          .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

      kmf.init(ks, keyManagerPass.toCharArray)

      val context = SSLContext.getInstance("TLS")
      context.init(kmf.getKeyManagers, Array(trustAllMgr), null)

      context
    }

  val trustAllMgr: TrustManager = new X509TrustManager {

    override def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}

    override def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}

    override def getAcceptedIssuers(): Array[X509Certificate] = Array()
  }

  def redirectApp[F[_]: Sync](securePort: Int): HttpApp[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpApp[F] { request =>
      request.headers.get(Host) match {
        case Some(Host(host @ _, _)) =>
          val baseUri = request.uri.copy(
            scheme = Scheme.https.some,
            authority = Some(
              Authority(
                userInfo = request.uri.authority.flatMap(_.userInfo),
                host = RegName(host),
                port = securePort.some)))
          MovedPermanently(Location(baseUri.withPath(request.uri.path)))
        case _ =>
          BadRequest()
      }
    }
  }
}

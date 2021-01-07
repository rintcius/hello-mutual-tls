package com.example.quickstart

import cats.effect._
import cats.syntax.all._

import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.File

object ClientExample extends IOApp {

  def context1[F[_]](implicit F: Sync[F]): F[SslContext] = F.delay {
    SslContextBuilder.forClient()
      .keyManager(new File("./src/main/resources/combi/client-cert.pem"), new File("./src/main/resources/combi/client-priv.pem"))
      .trustManager(new File("./src/main/resources/combi/client-cert.pem"))
      .build
  }

  def context2[F[_]](implicit F: Sync[F]): F[SslContext] =
    for {
      kmf <- ssl.keyManagerFactory[F](ssl.keystorePassword, ssl.keyManagerPassword, ssl.combiPath)
      ctx <-
        F.delay {
          SslContextBuilder.forClient()
            .keyManager(kmf)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build
        }
    } yield ctx

  def context3[F[_]](implicit F: Sync[F]): F[SslContext] =
    for {
      kmf <- ssl.keyManagerFactory[F](ssl.keystorePassword, ssl.keyManagerPassword, ssl.combiPathP12)
      ctx <-
        F.delay {
        SslContextBuilder.forClient()
          .keyManager(kmf)
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build
      }
    } yield ctx

  def exec(client: Client[IO]): IO[Unit] =
    IO {
      val res: IO[String] = client.expect[String](Uri.uri("https://localhost:8080/hello/world"))

      println(res.unsafeRunSync())
    }

  def run(args: List[String]): IO[ExitCode] = {

    for {
      ctx <- context1[IO]
      // ctx <- context2[IO]
      // ctx <- context3[IO]
      cfg =
        new DefaultAsyncHttpClientConfig.Builder()
          .setSslContext(ctx)
          .setDisableHttpsEndpointIdentificationAlgorithm(true)
          .build
      client = AsyncHttpClient.resource[IO](cfg)
      code <-
        client
          .use(exec)
          .as(ExitCode.Success)
    } yield code
  }
}

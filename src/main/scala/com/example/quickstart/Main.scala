package com.example.quickstart

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {

  def run(args: List[String]) =
    QuickstartServer.stream[IO](args(0).toBoolean).compile.drain.as(ExitCode.Success)

}

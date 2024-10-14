package com.thinkiny

import cats.effect.IOApp
import cats.effect.IO
import com.thinkiny.service.implicits.given
import com.thinkiny.service.syntax.*
import com.thinkiny.domain.RemoteFilePath

object Main extends IOApp.Simple:
  override def run: IO[Unit] =
    RemoteFilePath("root@jad#60022:~") match
      case Some(p) => p.getState[IO].map(println)
      case _       => IO.unit

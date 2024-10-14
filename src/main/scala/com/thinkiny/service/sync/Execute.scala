package com.thinkiny.service.sync

import cats.Applicative
import cats.effect.IO
import fs2.io.process.*
import fs2.text
import cats.syntax.all.*

trait Execute[F[_]]:
  def run(cmd: String): F[SyncResult[Unit]]

object Execute:
  def apply[F[_]](using ev: Execute[F]) = ev

  def show[F[_]: Applicative]: Execute[F] = new {
    override def run(cmd: String): F[SyncResult[Unit]] =
      cmd.pure[F].map(i => Right(println(s"show: ${i}")))
  }

  def dsl: Execute[IO] = new {
    def writeOutput(process: Process[IO]): IO[Unit] =
      process.stdout
        .through(fs2.io.stdout)
        .compile
        .drain

    def checkReturnCode(process: Process[IO]): IO[SyncResult[Unit]] =
      (
        process.stderr.through(text.utf8.decode).compile.string,
        process.exitValue
      )
        .parMapN {
          case (_, 0) => Right(())
          case (s, _) => Left(s)
        }

    override def run(cmd: String): IO[SyncResult[Unit]] =
      IO.println(s"run: ${cmd}") >>
        ProcessBuilder("bash", List("-c", cmd)).spawn[IO].use { process =>
          writeOutput(process) >> checkReturnCode(process)
        }
  }

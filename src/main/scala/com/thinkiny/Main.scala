package com.thinkiny

import cats.effect.ExitCode
import cats.effect.IO
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import com.thinkiny.cli.*

object Main extends CommandIOApp(name = "cosmic", header = "manage files"):
  def runCommands[F[_]](cmds: CliCommand[?, F]*): Opts[F[Unit]] =
    cmds.foldLeft(Opts.never)((b, a) => b.orElse(a.run()))

  override def main: Opts[IO[ExitCode]] =
    runCommands(SyncCommand, LsCommand).map(_.as(ExitCode.Success))

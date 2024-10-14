package com.thinkiny.cli

import com.monovore.decline.Opts

trait CliCommand[T, F[_]]:
  def options: Opts[T]
  def run(args: T): F[Unit]
  def run(): Opts[F[Unit]] = options.map(run)

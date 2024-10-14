package com.thinkiny.cli

import cats.data.Validated
import cats.effect.IO
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.thinkiny.domain.MergeStrategy
import com.thinkiny.implicits.given
import com.thinkiny.service.sync.RsyncFile

case class SyncOptions(
    src: String,
    dest: String,
    delete: Boolean = false,
    dry: Boolean = false
)

object SyncCommand extends CliCommand[SyncOptions, IO]:
  override def options: Opts[SyncOptions] =
    Opts.subcommand("sync", "sync files") {
      val delete =
        Opts.flag("delete", "delete extraneous files in dest dirs").orFalse
      val dry =
        Opts.flag("dry", "just report the actions about to make").orFalse
      val files =
        Opts.arguments[String]("<SRC> <DEST>").mapValidated { args =>
          if args.size == 2 then Validated.valid(args)
          else Validated.invalidNel(s"invalid arguments")
        }
      (files, delete, dry).mapN((f, del, dry) =>
        SyncOptions(f.head, f.last, del, dry)
      )
    }

  override def run(args: SyncOptions): IO[Unit] =
    val actions = List.newBuilder[MergeStrategy]
    if args.dry then actions += MergeStrategy.DryRun
    if args.delete then actions += MergeStrategy.Same
    else actions += MergeStrategy.Newer
    RsyncFile[IO]
      .sync(args.src, args.dest, actions.result()*)
      .flatMap {
        case Left(s) => IO.println(s"sync failed, ${s}")
        case _       => IO.println(s"sync success")
      }

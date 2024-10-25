package com.thinkiny.cli

import cats.data.Validated
import cats.effect.IO
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.thinkiny.domain.SyncOption
import com.thinkiny.implicits.given
import com.thinkiny.service.sync.RsyncFile
import com.thinkiny.domain.FilePath
import cats.data.NonEmptyList

case class SyncFileArgs(
    src: FilePath,
    dest: FilePath,
    delete: Boolean,
    dry: Boolean,
    verbose: Boolean,
    excludes: List[String]
)

object SyncFileCommand extends CliCommand[SyncFileArgs, IO]:
  def parsePathList(
      args: NonEmptyList[String]
  ): Option[NonEmptyList[FilePath]] =
    for
      src <- FilePath(args.head)
      dest <- FilePath(args.last)
    yield NonEmptyList(src, List(dest))

  override def options: Opts[SyncFileArgs] =
    Opts.subcommand("sync", "sync files") {
      val delete =
        Opts.flag("delete", "delete extraneous files in dest dirs", "d").orFalse
      val dry =
        Opts.flag("dry", "just report the actions about to make", "n").orFalse
      val verbose =
        Opts
          .flag("verbose", "just report the actions about to make", "v")
          .orFalse
      val excludes =
        Opts.options[String]("exclude", "exclude specific dirs", "e").orEmpty
      val files =
        Opts.arguments[String]("<SRC> <DEST>").mapValidated { args =>
          if args.size == 2 then
            parsePathList(args) match
              case Some(v) => Validated.valid(v)
              case _       => Validated.invalidNel(s"invalid path arguments")
          else Validated.invalidNel(s"missing path arguments")
        }
      (files, delete, dry, verbose, excludes).mapN(
        (f, del, dry, verbose, excludes) =>
          SyncFileArgs(f.head, f.last, del, dry, verbose, excludes)
      )
    }

  override def run(args: SyncFileArgs): IO[Unit] =
    val options = List.newBuilder[SyncOption]
    if args.dry then options += SyncOption.DryRun
    if args.verbose then options += SyncOption.Verbose
    if args.delete then options += SyncOption.KeepSync
    else options += SyncOption.UseNew
    options ++= args.excludes.map(SyncOption.Exclude(_))

    RsyncFile[IO]
      .sync(args.src, args.dest, options.result()*)
      .flatMap {
        case Left(s) => IO.println(s"sync failed, ${s}")
        case _       => IO.println(s"sync success")
      }

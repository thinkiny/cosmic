package com.thinkiny.service.sync

import com.thinkiny.domain.FilePath
import com.thinkiny.service.location.LocationPath
import com.thinkiny.domain.FileState
import cats.syntax.all.*
import cats.Monad
import com.thinkiny.domain.SyncOption

object RsyncFile:
  def apply[F[_]: LocationPath: Execute: Monad] = new SyncFile[F] {
    override def sync(
        src: FilePath,
        dest: FilePath,
        options: SyncOption*
    ): F[Error[Unit]] =
      LocationPath[F].getState(src).flatMap {
        case Some(stat) =>
          Execute[F].runM(makeCommand(src, dest, stat, options*))
        case _ =>
          Left(s"path doesn't exist: ${src}").pure[F]
      }

    def makeCommand(
        src: FilePath,
        dest: FilePath,
        srcState: FileState,
        options: SyncOption*
    ): F[String] =
      LocationPath[F].listFiles(src).map { files =>
        val builder = RsyncCmdBuilder(src, dest, srcState.isDir)
        builder.setProjectRoot(files)
        options.foreach:
          case SyncOption.UseNew   => builder += "-u"
          case SyncOption.KeepSync => builder += "--delete"
          case SyncOption.DryRun   => builder += "-n"
          case SyncOption.Verbose  => builder += "-v"
        builder.build()
      }
  }

package com.thinkiny.service.sync

import com.thinkiny.domain.FilePath
import com.thinkiny.domain.MergeStrategy
import com.thinkiny.service.location.LocationPath
import com.thinkiny.domain.FileState
import cats.syntax.all.*
import cats.Monad

object RsyncFile:
  def apply[F[_]: LocationPath: Execute: Monad] = new SyncFile[F] {
    override def sync(
        src: FilePath,
        dest: FilePath,
        actions: MergeStrategy*
    ): F[Error[Unit]] =
      LocationPath[F].getState(src).flatMap {
        case Some(stat) =>
          Execute[F].runM(makeCommand(src, dest, stat, actions*))
        case _ =>
          Left(s"path doesn't exist: ${src}").pure[F]
      }

    def makeCommand(
        src: FilePath,
        dest: FilePath,
        srcState: FileState,
        actions: MergeStrategy*
    ): F[String] =
      LocationPath[F].listFiles(src).map { files =>
        val builder = RsyncCmdBuilder(src, dest, srcState.isDir)
        builder.setProjectRoot(files)
        actions.foreach:
          case MergeStrategy.Newer  => builder += "-u"
          case MergeStrategy.Same   => builder += "--delete"
          case MergeStrategy.DryRun => builder += "-n"
        builder.build()
      }
  }

package com.thinkiny.service.sync

import com.thinkiny.domain.FilePath
import com.thinkiny.domain.MergeStrategy
import com.thinkiny.service.location.LocationFile
import com.thinkiny.domain.FileState
import cats.syntax.all.*
import cats.Monad

object RsyncFile:
  def apply[F[_]: LocationFile: Execute: Monad] = new SyncFile[F] {
    override def sync(
        src: FilePath,
        dest: FilePath,
        actions: MergeStrategy*
    ): F[SyncResult[Unit]] =
      LocationFile[F].getState(src).flatMap {
        case Some(stat) =>
          Execute[F].run(makeCommand(src, dest, stat, actions*))
        case _ =>
          Left(s"path doesn't exist: ${src}").pure[F]
      }

    def makeCommand(
        src: FilePath,
        dest: FilePath,
        srcState: FileState,
        actions: MergeStrategy*
    ): String =
      val builder = RsyncCmdBuilder(src, dest, srcState)
      builder += "-avr"
      builder += "--progress"
      builder += "-e 'ssh -T -c aes128-ctr -o Compression=no -x'"
      actions.foreach:
        case MergeStrategy.Newer  => builder += "-u"
        case MergeStrategy.Same   => builder += "--delete"
        case MergeStrategy.DryRun => builder += "-n"
      builder.build()
  }

package com.thinkiny.service.sync

import com.thinkiny.domain.FilePath
import com.thinkiny.domain.SyncOption

type Error[A] = Either[String, A]

trait SyncFile[F[_]: Execute]:
  def sync(
      src: FilePath,
      dest: FilePath,
      options: SyncOption*
  ): F[Error[Unit]]

object SyncFile:
  def apply[F[_]](using ev: SyncFile[F]) = ev

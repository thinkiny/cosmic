package com.thinkiny.service.sync

import com.thinkiny.domain.FilePath
import com.thinkiny.domain.MergeStrategy
import cats.Applicative

type SyncResult[A] = Either[String, A]

trait SyncFile[F[_]: Execute: Applicative]:
  private def parseFilePath(
      str: String,
      error: String
  ): SyncResult[FilePath] =
    FilePath(str) match
      case Some(x) => Right(x)
      case _       => Left(s"${error}: ${str}")

  def sync(
      src: FilePath,
      dest: FilePath,
      actions: MergeStrategy*
  ): F[SyncResult[Unit]]

  def sync(
      src: String,
      dest: String,
      actions: MergeStrategy*
  ): F[SyncResult[Unit]] = {
    val validatePath = for
      s <- parseFilePath(src, "invalid source")
      d <- parseFilePath(dest, "invalid destination")
    yield sync(s, d, actions*)

    validatePath match
      case Right(x) => x
      case Left(e)  => Applicative[F].pure(Left(e))
  }

object SyncFile:
  def apply[F[_]](using ev: SyncFile[F]) = ev

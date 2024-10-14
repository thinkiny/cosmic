package com.thinkiny.service

import com.thinkiny.domain.*

trait Location[F[_], A]:
  def getState(p: A): F[Option[FileState]]

trait LocalLocation[F[_]] extends Location[F, LocalFilePath]
trait RemoteLocation[F[_]] extends Location[F, RemoteFilePath]

object Location:
  def getState[F[_]](p: FilePath)(using
      local: LocalLocation[F],
      remote: RemoteLocation[F]
  ): F[Option[FileState]] =
    p match
      case l: LocalFilePath  => local.getState(l)
      case r: RemoteFilePath => remote.getState(r)

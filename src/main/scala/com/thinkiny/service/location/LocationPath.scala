package com.thinkiny.service.location

import com.thinkiny.domain.*

type LocationPath = [F[_]] =>> Location[F, FilePath]

object LocationPath:
  def apply[F[_]](using ev: LocationPath[F]) = ev

  def dsl[F[_]](using
      local: LocalLocation[F],
      remote: RemoteLocation[F]
  ): LocationPath[F] =
    new {
      override def getState(p: FilePath): F[Option[FileState]] =
        p match
          case l: LocalPath  => local.getState(l)
          case r: RemotePath => remote.getState(r)

      override def listFiles(p: FilePath): F[List[FileState]] =
        p match
          case l: LocalPath  => local.listFiles(l)
          case r: RemotePath => remote.listFiles(r)
    }

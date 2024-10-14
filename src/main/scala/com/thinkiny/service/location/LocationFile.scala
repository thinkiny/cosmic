package com.thinkiny.service.location

import com.thinkiny.domain.*

type LocationFile = [F[_]] =>> Location[F, FilePath]

object LocationFile:
  def apply[F[_]](using ev: LocationFile[F]) = ev

  def dsl[F[_]](using
      local: LocalLocation[F],
      remote: RemoteLocation[F]
  ): LocationFile[F] =
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

package com.thinkiny.service.location

import com.thinkiny.domain.*

trait Location[F[_], A]:
  def getState(p: A): F[Option[FileState]]
  def listFiles(p: A): F[List[FileState]]

trait LocalLocation[F[_]] extends Location[F, LocalPath]
trait RemoteLocation[F[_]] extends Location[F, RemotePath]

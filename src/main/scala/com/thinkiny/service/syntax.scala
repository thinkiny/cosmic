package com.thinkiny.service

import com.thinkiny.domain.*

object syntax:
  extension (p: FilePath)
    def getState[F[_]: LocalLocation: RemoteLocation]: F[Option[FileState]] =
      Location.getState[F](p)

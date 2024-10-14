package com.thinkiny.service

import cats.effect.IO
import com.thinkiny.domain.*
import fs2.io.file.Files
import fs2.io.file.Path

object LocalLocationIO:
  def dsl: LocalLocation[IO] = new {
    def makeFileState(p: Path): IO[FileState] =
      for
        isDir <- Files[IO].isDirectory(p)
        size <- Files[IO].size(p)
        mtime <- Files[IO].getLastModifiedTime(p)
      yield FileState(isDir, size, mtime.toSeconds)

    def getState(p: LocalFilePath): IO[Option[FileState]] =
      val path = Path(p.path)
      Files[IO].exists(path).ifM(makeFileState(path).map(Some(_)), IO.none)
  }

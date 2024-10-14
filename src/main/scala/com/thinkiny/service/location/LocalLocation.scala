package com.thinkiny.service.location

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
      yield FileState(isDir, size, mtime.toSeconds, p.absolute.toString)

    override def getState(p: LocalPath): IO[Option[FileState]] =
      val path = Path(p.path)
      Files[IO]
        .exists(path)
        .ifM(
          makeFileState(path).map(Some(_)),
          IO.none
        )

    override def listFiles(p: LocalPath): IO[List[FileState]] =
      val path = Path(p.path)
      Files[IO]
        .isDirectory(path)
        .ifM(
          Files[IO].list(path).evalMap(makeFileState(_)).compile.toList,
          makeFileState(path).map(List(_))
        )
  }

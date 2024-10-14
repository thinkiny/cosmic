package com.thinkiny.domain

sealed trait FilePath
case class RemoteFilePath(remote: RemoteHost, path: String) extends FilePath
case class LocalFilePath(path: String) extends FilePath

object RemoteFilePath:
  private def makeRemoteFilePath(
      user: String,
      host: String,
      port: Int,
      p: String
  ): RemoteFilePath =
    RemoteFilePath(
      remote = RemoteHost(user, host, port),
      path = p
    )

  def apply(s: String): Option[RemoteFilePath] =
    s match
      case s"${user}@${host}#${port}:${path}" =>
        port.toIntOption.map { p =>
          makeRemoteFilePath(user, host, p, path)
        }

      case s"${user}@${host}:${path}" =>
        Some(makeRemoteFilePath(user, host, 22, path))
      case _ => None

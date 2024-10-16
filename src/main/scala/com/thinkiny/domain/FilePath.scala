package com.thinkiny.domain

sealed trait FilePath
case class RemotePath(remote: RemoteHost, path: String) extends FilePath:
  override def toString(): String = s"${remote}:${path}"

case class LocalPath(path: String) extends FilePath:
  override def toString(): String = path

object RemotePath:
  private def makeRemoteFilePath(
      user: String,
      host: String,
      port: Int,
      p: String
  ): RemotePath =
    RemotePath(
      remote = RemoteHost(user, host, port),
      path = p
    )

  def apply(s: String): Option[RemotePath] =
    s match
      case s"${user}@${host}#${port}:${path}" =>
        port.toIntOption.map { p =>
          makeRemoteFilePath(user, host, p, path)
        }
      case s"${user}@${host}:${path}" =>
        Some(makeRemoteFilePath(user, host, -1, path))
      case s"${host}:${path}" =>
        Some(makeRemoteFilePath(null, host, -1, path))
      case _ => None

object FilePath:
  def apply(str: String): Option[FilePath] =
    if str.indexOf(":") == -1 then Some(LocalPath(str))
    else RemotePath(str)

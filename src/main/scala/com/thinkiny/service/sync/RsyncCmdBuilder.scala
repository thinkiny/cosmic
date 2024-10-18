package com.thinkiny.service.sync

import com.thinkiny.domain.*
import scala.collection.mutable.ListBuffer

class RsyncCmdBuilder(src: FilePath, dest: FilePath, srcIsDir: Boolean):
  private val flags = ListBuffer[String](
    "-arh",
    s"-e 'ssh -T -c aes128-ctr -o Compression=no -x ${getSshArg()}'",
    "--info=progress2" // mac need use: brew install rsync
  )

  private def getPort(src: FilePath): Option[Int] = src match
    case RemotePath(remote, _) if remote.port != -1 => Some(remote.port)
    case _                                          => None

  private def getSshArg(): String =
    getPort(src).orElse(getPort(dest)).map(p => s"-p ${p}").getOrElse("")

  def setProjectRoot(root: List[FileState]): Unit =
    Project
      .getExcludesFiles(root)
      .map { file =>
        s"--exclude '${file}'"
      }
      .foreach(flags += _)

  def formatFilePath(p: FilePath): String = p match
    case RemotePath(RemoteHost(null, host, _), path) =>
      s"${host}:${path}"
    case RemotePath(RemoteHost(user, host, _), path) =>
      s"${user}@${host}:${path}"
    case LocalPath(path) => path

  def +=(flag: String): Unit =
    flags += flag

  def build(): String = {
    val srcPath = formatFilePath(src)
    val srcArg =
      if srcIsDir && !srcPath.endsWith("/") then srcPath + "/"
      else srcPath
    s"rsync ${flags.mkString(" ")} ${srcArg} ${formatFilePath(dest)}"
  }

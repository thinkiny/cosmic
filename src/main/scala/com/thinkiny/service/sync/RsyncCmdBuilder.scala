package com.thinkiny.service.sync

import com.thinkiny.domain.*
import scala.collection.mutable.ListBuffer

class RsyncCmdBuilder(src: FilePath, dest: FilePath, srcState: FileState):
  private val flags = ListBuffer[String]()

  def formatFilePath(p: FilePath): String = p match
    case RemotePath(RemoteHost(user, host, _), path) =>
      s"${user}@${host}:${path}"
    case LocalPath(path) => path

  def +=(flag: String): Unit =
    flags += flag

  def build(): String = {
    val srcPath = formatFilePath(src)
    val srcArg =
      if srcState.isDir && !srcPath.endsWith("/") then srcPath + "/"
      else srcPath
    s"rsync ${flags.mkString(" ")} ${srcArg} ${formatFilePath(dest)}"
  }

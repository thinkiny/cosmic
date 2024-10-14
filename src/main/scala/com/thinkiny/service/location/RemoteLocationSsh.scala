package com.thinkiny.service.location

import cats.effect.IO
import com.thinkiny.ssh.Ssh
import com.thinkiny.domain.*
import com.jcraft.jsch.Session

object RemoteLocationSsh:
  def makeFileState(
      fileMode: String,
      bytes: String,
      time: String,
      path: String
  ): FileState =
    FileState(
      fileMode.startsWith("d"),
      bytes.toLong,
      time.toDouble.toLong,
      path
    )

  def parseStatOutput(output: String): Option[FileState] =
    output.trim().split("\t") match
      case Array(fileMode, bytes, time, path) =>
        Some(makeFileState(fileMode, bytes, time, path))
      case _ => None

  def parseListFileOutput(output: String): List[FileState] =
    output.trim().split("\n").flatMap(parseStatOutput(_)).to(List)

  def dsl(using ssh: Ssh[IO]): RemoteLocation[IO] = new {
    def executePython(p: RemotePath, cmd: String): IO[String] =
      ssh.connect(p.remote.user, p.remote.host, p.remote.port).flatMap {
        case Some(session) =>
          PythonScript.execute(ssh, session, cmd)
        case _ =>
          IO.raiseError(new RuntimeException(s"can't connect to ${p.remote}"))
      }

    override def getState(p: RemotePath): IO[Option[FileState]] =
      executePython(p, s"stat_file('${p.path}')").map(parseStatOutput)

    override def listFiles(p: RemotePath): IO[List[FileState]] =
      executePython(p, s"list_files('${p.path}')").map(parseListFileOutput)
  }

  private object PythonScript:
    val template = """import os, stat

def stat_file(p):
    path = os.path.abspath(os.path.expanduser(p))
    s = os.lstat(path)
    mode = s.st_mode
    file_mode = "f"
    if stat.S_ISDIR(mode):
        file_mode = "d"
    print("{}\t{}\t{}\t{}".format(file_mode, s.st_size, s.st_mtime, path))

def list_files(d):
    path = os.path.abspath(os.path.expanduser(d))
    if os.path.isdir(path):
      for f in os.listdir(path):
            stat_file(os.path.join(path, f))
    else:
      stat_file(d)

__SCRIPT__"""
    def execute(ssh: Ssh[IO], session: Session, script: String): IO[String] =
      ssh.execScript(session, "python3", template.replace("__SCRIPT__", script))

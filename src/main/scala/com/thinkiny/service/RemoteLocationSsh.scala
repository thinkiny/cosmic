package com.thinkiny.service

import cats.effect.IO
import com.thinkiny.ssh.Ssh
import com.thinkiny.domain.*
import com.jcraft.jsch.Session

object RemoteLocationSsh:
  def makeFileState(fileMode: String, bytes: String, time: String): FileState =
    FileState(fileMode.startsWith("d"), bytes.toLong, time.toDouble.toLong)

  def parseStatOutput(output: String): Option[FileState] =
    output.trim().split("\t") match
      case Array(fileMode, bytes, time, _*) =>
        Some(makeFileState(fileMode, bytes, time))
      case _ => None

  def dsl(using ssh: Ssh[IO]): RemoteLocation[IO] = new {
    def getState(p: RemoteFilePath): IO[Option[FileState]] =
      ssh.connect(p.remote.user, p.remote.host, p.remote.port).flatMap {
        case Some(session) =>
          PythonScript
            .execute(ssh, session, s"stat_file('${p.path}')")
            .map(parseStatOutput)
        case _ =>
          IO.raiseError(new RuntimeException(s"can't connect to ${p.remote}"))
      }
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
    for f in os.listdir(os.path.expanduser(d)):
        stat_file(os.path.join(d, f))

__SCRIPT__"""
    def execute(ssh: Ssh[IO], session: Session, script: String): IO[String] =
      ssh.execScript(session, "python3", template.replace("__SCRIPT__", script))

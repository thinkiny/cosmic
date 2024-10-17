package com.thinkiny.ssh

import com.jcraft.jsch.ChannelExec
import java.io.InputStream
import com.jcraft.jsch.JSch
import java.util.Properties
import cats.effect.kernel.Resource
import cats.effect.IO
import com.jcraft.jsch.Session
import scala.util.Try
import scala.collection.mutable.HashMap
import java.nio.file.Files
import java.nio.file.Paths
import com.jcraft.jsch.OpenSSHConfig

object SshIO:
  val home = System.getProperty("user.home")
  val sshConfFile = s"${home}/.ssh/config"
  val privateKeyFile = s"${home}/.ssh/id_rsa"

  def createJsch(): JSch = {
    val jsch = new JSch
    jsch.addIdentity(privateKeyFile)
    if Files.exists(Paths.get(sshConfFile)) then
      val sshConfig = OpenSSHConfig.parseFile(sshConfFile)
      jsch.setConfigRepository(sshConfig)
    jsch
  }

  def createSession(
      user: String,
      host: String,
      port: Int
  ): Option[Session] =
    Try {
      val jsch = createJsch()
      val session = jsch.getSession(user, host, port)
      val config = new Properties()

      if session.getPort() == -1 then session.setPort(22)
      config.put("StrictHostKeyChecking", "no")
      session.setConfig(config)
      session.setTimeout(30000) // milliseconds
      session.connect()
      session
    }.toOption

  def createChannelExec(session: Session): Resource[IO, ChannelExec] =
    Resource.make(IO {
      session.openChannel("exec").asInstanceOf[ChannelExec]
    }) { c =>
      IO.blocking(c.disconnect())
    }

  def sendCommand(
      channel: ChannelExec,
      cmd: String
  ): Resource[IO, InputStream] =
    Resource.make(IO {
      channel.setCommand(cmd)
      channel.setInputStream(null)
      channel.connect()
      channel.getInputStream()
    }) { input =>
      IO.blocking(input.close())
    }

  def readCommandOutput(
      session: Session,
      cmd: String
  ): Resource[IO, InputStream] =
    for
      exec <- createChannelExec(session)
      inputStream <- sendCommand(exec, cmd)
    yield inputStream

  def dsl: Ssh[IO] = {
    val cache = new HashMap[String, Session]
    new Ssh[IO] {
      override def connect(
          user: String,
          host: String,
          port: Int
      ): IO[Option[Session]] =
        IO.blocking {
          cache.synchronized {
            val key = s"${user}@${host}#${port}"
            if cache.contains(key) then Some(cache(key))
            else
              val res = createSession(user, host, port)
              res.foreach(cache.put(key, _))
              res
          }
        }

      override def exec(session: Session, cmd: String): IO[String] =
        fs2.Stream
          .resource[IO, InputStream](readCommandOutput(session, cmd))
          .flatMap(s => fs2.io.readInputStream(IO.pure(s), 1024, false))
          .through(fs2.text.utf8.decode)
          .compile
          .foldMonoid
    }
  }

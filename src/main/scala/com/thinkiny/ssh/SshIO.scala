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

object SshIO:
  def createSession(
      user: String,
      host: String,
      port: Int
  ): Option[Session] =
    Try {
      val jsch = new JSch
      val session = jsch.getSession(user, host, port)
      val config = new Properties()

      jsch.addIdentity(s"${System.getProperty("user.home")}/.ssh/id_rsa")
      config.put("StrictHostKeyChecking", "no")
      session.setConfig(config)
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

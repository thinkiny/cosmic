package com.thinkiny.service

import cats.effect.IO
import com.thinkiny.ssh.SshIO
import com.thinkiny.ssh.Ssh

object implicits:
  given Ssh[IO] = SshIO.dsl
  given LocalLocation[IO] = LocalLocationIO.dsl
  given RemoteLocation[IO] = RemoteLocationSsh.dsl

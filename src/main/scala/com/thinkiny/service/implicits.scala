package com.thinkiny.service

import cats.effect.IO
import com.thinkiny.ssh.SshIO
import com.thinkiny.ssh.Ssh
import com.thinkiny.service.location.*
import com.thinkiny.service.sync.Execute

trait ServiceImplicits:
  given Ssh[IO] = SshIO.dsl
  given LocalLocation[IO] = LocalLocationIO.dsl
  given RemoteLocation[IO] = RemoteLocationSsh.dsl
  given LocationPath[IO] = LocationPath.dsl
  given Execute[IO] = Execute.dsl

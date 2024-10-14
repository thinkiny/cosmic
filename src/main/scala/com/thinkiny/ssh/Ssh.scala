package com.thinkiny.ssh

import com.jcraft.jsch.Session

trait Ssh[F[_]]:
  def connect(user: String, host: String, port: Int): F[Option[Session]]
  def exec(session: Session, cmd: String): F[String]
  def execScript(session: Session, program: String, script: String): F[String] =
    exec(
      session,
      s"""${program} - <<EOF
       |${script}
       |EOF""".stripMargin
    )

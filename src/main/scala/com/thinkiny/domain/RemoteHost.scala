package com.thinkiny.domain

case class RemoteHost(user: String, host: String, port: Int):
  override def toString(): String =
    val userHead = Option(user).map(u => s"${u}@").getOrElse("")
    if port != -1 then s"${userHead}${host}#${port}"
    else s"${userHead}${host}"

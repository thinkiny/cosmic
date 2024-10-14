package com.thinkiny.domain

case class RemoteHost(user: String, host: String, port: Int):
  override def toString(): String =
    if port != -1 then s"${user}@${host}#${port}"
    else s"${user}@${host}"

  def /(path: String): RemotePath = RemotePath(this, path)

package com.thinkiny

object syntax:
  extension (s: String)
    def getFileName: String =
      s.substring(s.lastIndexOf('/') + 1)

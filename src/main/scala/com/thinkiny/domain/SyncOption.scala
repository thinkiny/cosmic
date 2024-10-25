package com.thinkiny.domain

enum SyncOption:
  case KeepSync, UseNew, DryRun, Verbose
  case Exclude(path: String) extends SyncOption

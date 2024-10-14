package com.thinkiny.domain

case class SyncTask(from: FilePath, to: FilePath, onConflict: MergeStrategy)

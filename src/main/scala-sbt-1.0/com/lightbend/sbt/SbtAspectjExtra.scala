package com.lightbend.sbt

import sbt._
import sbt.io.Path

object SbtAspectjExtra {
  def copyResources(
      ajcInputs: Seq[File],
      compileClassDir: File,
      resourceMappings: Seq[(File, File)],
      aspectjClassDir: File,
      taskStreams: Keys.TaskStreams): Seq[(File, File)] = {
    val cacheStore = taskStreams.cacheStoreFactory make "aspectj-resource-sync"
    val mapped = if (ajcInputs contains compileClassDir) {
      resourceMappings map (_._2) pair Path.rebase(compileClassDir, aspectjClassDir)
    } else Seq.empty
    Sync(cacheStore)(mapped)
    mapped
  }
}

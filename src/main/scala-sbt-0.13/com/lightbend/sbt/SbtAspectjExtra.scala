package com.lightbend.sbt

import sbt._

object SbtAspectjExtra {
  def copyResources(
      ajcInputs: Seq[File],
      compileClassDir: File,
      resourceMappings: Seq[(File, File)],
      aspectjClassDir: File,
      taskStreams: Keys.TaskStreams): Seq[(File, File)] = {
    val cacheFile = taskStreams.cacheDirectory / "aspectj-resource-sync"
    val mapped = if (ajcInputs contains compileClassDir) {
      resourceMappings map (_._2) pair rebase(compileClassDir, aspectjClassDir)
    } else Seq.empty
    Sync(cacheFile)(mapped)
    mapped
  }
}

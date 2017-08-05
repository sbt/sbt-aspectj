package com.lightbend.sbt

import sbt._
import sbt.Keys._

trait AspectjKeys {
  val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")

  val aspectjDirectory = SettingKey[File]("aspectj-directory", "Target directory for AspectJ.")

  val showWeaveInfo = SettingKey[Boolean]("show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
  val verbose = SettingKey[Boolean]("verbose", "Enable the -verbose AspectJ option.")
  val compileOnly = SettingKey[Boolean]("compile-only", "The AspectJ -XterminateAfterCompilation option.")
  val outXml = SettingKey[Boolean]("out-xml", "Enable the -outxml AspectJ option.")
  val sourceLevel = SettingKey[String]("source-level", "The AspectJ source level option.")
  val lintProperties = SettingKey[Seq[String]]("lint-properties", "AspectJ -Xlint properties.")
  val lintPropertiesFile = TaskKey[Option[File]]("lint-properties-file", "Write any -Xlint properties to a file.")
  val extraAspectjOptions = TaskKey[Seq[String]]("extra-aspectj-options", "Extra AspectJ options (which don't have provided sbt settings).")
  val aspectjOptions = TaskKey[AspectjOptions]("aspectj-options", "Configurable AspectJ options.")

  val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
  val inputs = TaskKey[Seq[File]]("inputs", "Jars or class directories to weave. Passed to the -inpath AspectJ option.")
  val binaries = TaskKey[Seq[File]]("binaries", "Binary aspects passed to the -aspectpath AspectJ option.")
  val output = TaskKey[File]("output", "The output class directory or jar file for AspectJ.")
  val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath", "The classpath used for running AspectJ.")
  val compiledClasses = TaskKey[File]("compiled-classes", "List of compiled AspectJ compiled classes.")

  val ajc = TaskKey[File]("ajc", "Run the AspectJ compiler.")
  val weave = TaskKey[File]("weave", "Weave with AspectJ.")

  val weaver = TaskKey[Option[File]]("weaver", "Location of AspectJ load-time weaver.")
  val weaverOptions = TaskKey[Seq[String]]("weaver-options", "JVM options for AspectJ weaver java agent.")
}

object AspectjKeys extends AspectjKeys

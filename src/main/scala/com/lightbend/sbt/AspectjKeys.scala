package com.lightbend.sbt

import sbt._
import sbt.Keys._

trait AspectjKeys {
  val aspectjVersion = settingKey[String]("AspectJ version to use.")

  val aspectjDirectory = settingKey[File]("Target directory for AspectJ.")

  val aspectjShowWeaveInfo = settingKey[Boolean]("Enable the -showWeaveInfo AspectJ option.")
  val aspectjVerbose = settingKey[Boolean]("Enable the -verbose AspectJ option.")
  val aspectjCompileOnly = settingKey[Boolean]("The AspectJ -XterminateAfterCompilation option.")
  val aspectjOutXml = settingKey[Boolean]("Enable the -outxml AspectJ option.")
  val aspectjSourceLevel = settingKey[String]("The AspectJ source level option.")
  val aspectjLintProperties = settingKey[Seq[String]]("AspectJ -Xlint properties.")
  val aspectjLintPropertiesFile = taskKey[Option[File]]("Write any -Xlint properties to a file.")
  val aspectjExtraOptions = taskKey[Seq[String]]("Extra AspectJ options (which don't have provided sbt settings).")
  val aspectjOptions = taskKey[AspectjOptions]("Configurable AspectJ options.")

  val aspectjSource = settingKey[File]("Source directory for aspects.")
  val aspectjInputs = taskKey[Seq[File]]("Jars or class directories to weave. Passed to the -inpath AspectJ option.")
  val aspectjBinaries = taskKey[Seq[File]]("Binary aspects passed to the -aspectpath AspectJ option.")
  val aspectjClasspath = taskKey[Classpath]("The classpath used for running AspectJ.")
  val aspectjCompiledClasses = taskKey[File]("List of compiled AspectJ compiled classes.")

  val ajc = taskKey[File]("Run the AspectJ compiler.")
  val aspectjWeave = taskKey[File]("Weave with AspectJ.")

  val aspectjWeaver = taskKey[Option[File]]("Location of AspectJ load-time weaver.")
  val aspectjWeaverOptions = taskKey[Seq[String]]("JVM options for AspectJ weaver java agent.")
}

object AspectjKeys extends AspectjKeys

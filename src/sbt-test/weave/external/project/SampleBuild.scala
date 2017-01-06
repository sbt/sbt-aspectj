package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, compiledClasses }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ binaries, compileOnly, inputs, lintProperties }

object SampleBuild extends Build {
  val Organization = "com.typesafe.sbt.aspectj.sample.external"
  val Version = "0.1-SNAPSHOT"

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version := Version,
    scalaVersion := "2.12.1"
  )

  lazy val sample = Project(
    "sample",
    file("."),
    settings = buildSettings,
    aggregate = Seq(tracer, instrumented)
  )

  // compiled aspects (published locally for this sample)
  lazy val tracer = Project(
    "tracer",
    file("tracer"),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // ignore warnings (we don't have the target classes at this point)
      lintProperties in Aspectj += "invalidAbsoluteTypeName = ignore",

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj
    )
  )

  // use the published tracer (as if it was external)
  lazy val instrumented = Project(
    "instrumented",
    file("instrumented"),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // add the compiled aspects as a dependency
      libraryDependencies += Organization %% "tracer" % Version,

      // add the tracer as binary aspects for aspectj
      binaries in Aspectj <++= update map { report =>
        report.matching(moduleFilter(organization = Organization, name = "tracer*"))
      },

      // weave this project's classes
      inputs in Aspectj <+= compiledClasses,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile
    )
  )
}

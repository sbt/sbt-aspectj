package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, compiledClasses }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ binaries, inputs, lintProperties }

object SampleBuild extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.typesafe.sbt.aspectj",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.12.1"
  )

  lazy val sample = Project(
    "sample",
    file("."),
    settings = buildSettings,
    aggregate = Seq(tracer, instrumented)
  )

  // precompiled annotation-based aspects
  lazy val tracer = Project(
    "tracer",
    file("tracer"),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // input compiled scala classes
      inputs in Aspectj <+= compiledClasses,

      // ignore warnings
      lintProperties in Aspectj += "invalidAbsoluteTypeName = ignore",
      lintProperties in Aspectj += "adviceDidNotMatch = ignore",

      // replace regular products with compiled aspects
      products in Compile <<= products in Aspectj
    )
  )

  // test that the instrumentation works
  lazy val instrumented = Project(
    "instrumented",
    file("instrumented"),
    dependencies = Seq(tracer),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // add the compiled aspects from tracer
      binaries in Aspectj <++= products in Compile in tracer,

      // weave this project's classes
      inputs in Aspectj <+= compiledClasses,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile
    )
  )
}

package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ binaries, compiledClasses, compileOnly, inputs, lintProperties }

object SampleBuild extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.typesafe.sbt.aspectj",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.1"
  )

  lazy val precompiled = Project(
    "precompiled",
    file("."),
    settings = buildSettings,
    aggregate = Seq(tracer, sample)
  )

  lazy val tracer = Project(
    "tracer",
    file("tracer"),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      compileOnly in Aspectj := true,
      lintProperties in Aspectj += "invalidAbsoluteTypeName = ignore",
      products in Compile <++= products in Aspectj
    )
  )

  lazy val sample = Project(
    "sample",
    file("sample"),
    dependencies = Seq(tracer),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      binaries in Aspectj <++= products in Aspectj in tracer,
      inputs in Aspectj <+= compiledClasses in Aspectj,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile
    )
  )
}

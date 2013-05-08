package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ compileOnly, weaverOptions }

object SampleBuild extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.typesafe.sbt.aspectj",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.1"
  )

  lazy val ltw = Project(
    "ltw",
    file("."),
    settings = buildSettings,
    aggregate = Seq(sample, tracer)
  )

  lazy val sample = Project(
    "sample",
    file("sample"),
    settings = buildSettings
  )

  lazy val tracer = Project(
    "tracer",
    file("tracer"),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      compileOnly in Aspectj := true,
      products in Compile <++= products in Aspectj,
      javaOptions in run in Test <++= weaverOptions in Aspectj,
      fork in run in Test := true
    ),
    dependencies = Seq(sample)
  )
}

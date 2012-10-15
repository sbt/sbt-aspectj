package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ enableProducts, weaveAgentOptions }

object SampleBuild extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.typesafe.sbt.aspectj",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.9.2"
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
      enableProducts in Aspectj := true,
      javaOptions in run in Test <++= weaveAgentOptions in Aspectj,
      fork in run in Test := true
    ),
    dependencies = Seq(sample)
  )
}

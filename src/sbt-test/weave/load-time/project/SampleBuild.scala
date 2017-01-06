package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ compileOnly, weaverOptions }

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
    aggregate = Seq(inputs, tracer)
  )

  lazy val inputs = Project(
    "inputs",
    file("inputs"),
    settings = buildSettings
  )

  lazy val tracer = Project(
    "tracer",
    file("tracer"),
    dependencies = Seq(inputs),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // only compile the aspects (no weaving)
      compileOnly in Aspectj := true,

      // add the compiled aspects as products
      products in Compile <++= products in Aspectj
    )
  )

  lazy val woven = Project(
    "woven",
    file("woven"),
    dependencies = Seq(inputs, tracer),
    settings = buildSettings ++ aspectjSettings ++ Seq(
      // fork the run so that javaagent option can be added
      fork in run := true,

      // add the aspectj weaver javaagent option
      javaOptions in run <++= weaverOptions in Aspectj
    )
  )
}

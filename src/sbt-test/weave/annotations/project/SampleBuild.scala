package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ compiledClasses, inputs, weave }

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ aspectjSettings ++ Seq(
      organization := "com.typesafe.sbt.aspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      inputs in Aspectj <+= compiledClasses in Aspectj,
      products in Compile <<= weave in Aspectj,
      products in Runtime <<= products in Compile
    )
  )
}

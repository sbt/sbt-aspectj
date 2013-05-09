package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, compiledClasses }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ inputs, weave }

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ aspectjSettings ++ Seq(
      organization := "com.typesafe.sbt.aspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.1",

      // add compiled classes as an input to aspectj
      inputs in Aspectj <+= compiledClasses,

      // use the results of aspectj weaving
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile
    )
  )
}

/**
 *  Copyright (C) 2011 Typesafe, Inc <http://typesafe.com>
 */

package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, useInstrumentedClasses }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.{ inputs, weave }

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ aspectjSettings ++ Seq(
      organization := "com.typesafe.sbtaspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      inputs in Aspectj <<= classDirectory in Compile map { Seq(_) },
      fullClasspath in Test <<= useInstrumentedClasses(Test),
      fullClasspath in Runtime <<= useInstrumentedClasses(Runtime),
      products in Compile <<= weave in Aspectj map identity
    )
  )
}

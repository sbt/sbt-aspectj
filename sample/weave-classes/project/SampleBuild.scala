/**
 *  Copyright (C) 2011 Typesafe, Inc <http://typesafe.com>
 */

package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbtaspectj.AspectjPlugin
import com.typesafe.sbtaspectj.AspectjPlugin.{ Aspectj, inputs, weave }

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ AspectjPlugin.settings ++ Seq(
      organization := "com.typesafe.sbtaspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.1",
      inputs in Aspectj <<= classDirectory in Compile map { Seq(_) },
      fullClasspath in Test <<= AspectjPlugin.useInstrumentedClasses(Test),
      fullClasspath in Runtime <<= AspectjPlugin.useInstrumentedClasses(Runtime),
      products in Compile <<= weave in Aspectj map identity
    )
  )
}

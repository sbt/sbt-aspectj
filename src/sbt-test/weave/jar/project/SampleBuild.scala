package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj.{ Aspectj, aspectjSettings, useInstrumentedJars }
import com.typesafe.sbt.SbtAspectj.AspectjKeys.inputs

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ aspectjSettings ++ Seq(
      organization := "com.typesafe.sbt.aspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.1",
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2",
      inputs in Aspectj <<= update map { report =>
        report.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))
      },
      fullClasspath in Runtime <<= useInstrumentedJars(Runtime)
    )
  )
}

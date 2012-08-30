/**
 *  Copyright (C) 2011 Typesafe, Inc <http://typesafe.com>
 */

package sample

import sbt._
import sbt.Keys._
import com.typesafe.sbtaspectj.AspectjPlugin
import com.typesafe.sbtaspectj.AspectjPlugin.{ Aspectj, inputs, aspectFilter }

object SampleBuild extends Build {
  lazy val sample = Project(
    id = "sample",
    base = file("."),
    settings = Defaults.defaultSettings ++ AspectjPlugin.settings ++ Seq(
      organization := "com.typesafe.sbtaspectj",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.2",
      inputs in Aspectj <<= update map { report =>
        report.matching(moduleFilter(organization = "se.scalablesolutions.akka", name = "akka-actor"))
      },
      aspectFilter in Aspectj := {
        (jar, aspects) => {
          if (jar.name.contains("akka-actor")) aspects filter (_.name.startsWith("Actor"))
          else Seq.empty[File]
        }
      },
      fullClasspath in Test <<= AspectjPlugin.useInstrumentedJars(Test),
      fullClasspath in Runtime <<= AspectjPlugin.useInstrumentedJars(Runtime)
    )
  )
}

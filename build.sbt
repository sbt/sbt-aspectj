
sbtPlugin := true

organization := "com.typesafe.sbtaspectj"

name := "sbt-aspectj"

version := "0.4.5-SNAPSHOT"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.6.11"

publishMavenStyle := false

publishTo := Option(Classpaths.typesafeResolver)

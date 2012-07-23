
sbtPlugin := true

organization := "com.typesafe.sbtaspectj"

name := "sbt-aspectj"

version := "0.5.2"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.6.12"

publishMavenStyle := false

publishTo := Option(Classpaths.typesafeResolver)

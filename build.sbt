
sbtPlugin := true

organization := "com.typesafe.sbt-aspectj"

name := "sbt-aspectj"

version := "0.4.4-SNAPSHOT"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.6.11"

publishMavenStyle := true

publishTo := Some("Typesafe Publish Repo" at "http://repo.typesafe.com/typesafe/maven-releases/")

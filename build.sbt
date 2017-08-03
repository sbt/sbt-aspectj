
sbtPlugin := true

organization := "com.typesafe.sbt"
name := "sbt-aspectj"
version := "0.11-SNAPSHOT"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.8.10"

publishMavenStyle := false

bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := name.value
bintrayReleaseOnPublish := false

scriptedSettings
scriptedDependencies := publishLocal.value
scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")

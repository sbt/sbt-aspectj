
sbtPlugin := true

organization := "com.typesafe"

name := "aspectj-sbt-plugin"

version := "0.4.0"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.6.11"

publishMavenStyle := true

publishTo := Some("Typesafe Publish Repo" at "http://repo.typesafe.com/typesafe/maven-releases/")

credentials += Credentials(Path.userHome / ".ivy2" / "typesafe-credentials")

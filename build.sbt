
sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-aspectj"

version := "0.10-SNAPSHOT"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.8.6"

publishMavenStyle := false

publishTo <<= isSnapshot { snapshot =>
  if (snapshot) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
}

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

CrossBuilding.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m", s"-Dproject.version=${version.value}")

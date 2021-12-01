
sbtPlugin := true

organization := "com.liyutech"
name := "sbt-aspectj"

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.9.6"

publishMavenStyle := true

scriptedDependencies := publishLocal.value
scriptedLaunchOpts ++= Seq("-Xms512m", "-Xmx512m", s"-Dproject.version=${version.value}")

crossSbtVersions := Vector("1.4.6", "1.1.5", "0.13.17")

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ scripted"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

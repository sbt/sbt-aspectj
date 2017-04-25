import com.typesafe.sbt.SbtAspectj.useInstrumentedClasses

organization := "com.typesafe.sbt.aspectj"
version := "0.1-SNAPSHOT"
scalaVersion := "2.12.1"

enablePlugins(SbtAspectj)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.16"

// add akka-actor as an aspectj input (find it in the update report)
inputs in Aspectj ++= update.value.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))

// replace the original akka-actor jar with the instrumented classes in runtime
fullClasspath in Runtime := useInstrumentedClasses(Runtime).value

// for sbt scripted test:
TaskKey[Unit]("check") := {
  val cp = (fullClasspath in Runtime).value
  val mc = (mainClass in Runtime).value
  val opts = (javaOptions in run in Compile).value

  val expected = "Actor asked world\nhello world\n"
  val output = Process("java", opts ++ Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) {
    println("Unexpected output:")
    println(output)
    println("Expected:")
    println(expected)
    sys.error("Unexpected output")
  } else {
    print(output)
  }
}

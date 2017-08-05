lazy val buildSettings = Seq(
  organization := "com.lightbend.sbt.aspectj",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.1"
)

lazy val sample = (project in file("."))
  .settings(buildSettings)
  .aggregate(tracer, instrumented)

// precompiled aspects
lazy val tracer = (project in file("tracer"))
  .enablePlugins(SbtAspectj)
  .settings(buildSettings)
  .settings(
    // input compiled scala classes
    inputs in Aspectj += (compiledClasses in Aspectj).value,

    // ignore warnings
    lintProperties in Aspectj += "invalidAbsoluteTypeName = ignore",
    lintProperties in Aspectj += "adviceDidNotMatch = ignore",

    // replace regular products with compiled aspects
    products in Compile := (products in Aspectj).value
  )

// test that the instrumentation works
lazy val instrumented = (project in file("instrumented"))
  .enablePlugins(SbtAspectj)
  .settings(buildSettings)
  .settings(
    // add the compiled aspects from tracer
    binaries in Aspectj ++= (products in Compile in tracer).value,

    // weave this project's classes
    inputs in Aspectj += (compiledClasses in Aspectj).value,
    products in Compile := (products in Aspectj).value,
    products in Runtime := (products in Compile).value
  ).dependsOn(tracer)

// for sbt scripted test:
TaskKey[Unit]("check") := {
  import scala.sys.process.Process

  val cp = (fullClasspath in Compile in instrumented).value
  val mc = (mainClass in Compile in instrumented).value
  val opts = (javaOptions in run in Compile in instrumented).value

  val expected = "Printing sample:\nhello\n"
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

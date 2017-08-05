val Organization = "com.lightbend.sbt.aspectj.sample.external"
val Version = "0.1-SNAPSHOT"

lazy val buildSettings = Seq(
  organization := Organization,
  version := Version,
  scalaVersion := "2.12.1"
)

lazy val sample = (project in file("."))
  .settings(buildSettings)
  .aggregate(tracer, instrumented)

// compiled aspects (published locally for this sample)
lazy val tracer = (project in file("tracer"))
  .enablePlugins(SbtAspectj)
  .settings(buildSettings)
  .settings(
    // only compile the aspects (no weaving)
    aspectjCompileOnly in Aspectj := true,

    // ignore warnings (we don't have the target classes at this point)
    aspectjLintProperties in Aspectj += "invalidAbsoluteTypeName = ignore",

    // replace regular products with compiled aspects
    products in Compile ++= (products in Aspectj).value
  )

// use the published tracer (as if it was external)
lazy val instrumented = (project in file("instrumented"))
  .enablePlugins(SbtAspectj)
  .settings(buildSettings)
  .settings(
    // add the compiled aspects as a dependency
    libraryDependencies += Organization %% "tracer" % Version,

    // add the tracer as binary aspects for aspectj
    aspectjBinaries in Aspectj ++= update.value.matching(moduleFilter(organization = Organization, name = "tracer*")),

    // weave this project's classes
    aspectjInputs in Aspectj += (aspectjCompiledClasses in Aspectj).value,
    products in Compile := (products in Aspectj).value,
    products in Runtime := (products in Compile).value
  )

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

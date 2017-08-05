
organization := "com.lightbend.sbt.aspectj"
version := "0.1-SNAPSHOT"
scalaVersion := "2.12.1"

enablePlugins(SbtAspectj)

// add compiled classes as an input to aspectj
inputs in Aspectj += (compiledClasses in Aspectj).value

// use the results of aspectj weaving
products in Compile := (products in Aspectj).value
products in Runtime := (products in Compile).value

// for sbt scripted test:
TaskKey[Unit]("check") := {
  import scala.sys.process.Process

  val cp = (fullClasspath in Compile).value
  val mc = (mainClass in Compile).value
  val opts = (javaOptions in run in Compile).value

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

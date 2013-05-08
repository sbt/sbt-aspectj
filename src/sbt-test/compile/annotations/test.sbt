// for sbt scripted test:
TaskKey[Unit]("check") <<= (
  fullClasspath in Compile in instrumented,
  mainClass in Compile in instrumented,
  javaOptions in run in Compile in instrumented
) map { (cp, mc, opts) =>
  val expected = "Printing sample:\nhello\n"
  val output = Process("java", opts ++ Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) {
    println("Unexpected output:")
    println(output)
    println("Expected:")
    println(expected)
    error("Unexpected output")
  } else {
    print(output)
  }
}

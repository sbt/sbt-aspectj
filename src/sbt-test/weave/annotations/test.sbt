// for sbt scripted test:
TaskKey[Unit]("check") <<= (
  fullClasspath in Runtime,
  mainClass in Runtime,
  javaOptions in run in Compile
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

// for sbt scripted test:
TaskKey[Unit]("check") <<= (fullClasspath in Compile in sample, mainClass in Compile in sample, javaOptions in run in Compile in sample) map { (cp, mc, opts) =>
  val expected = "Printing sample:\nhello\n"
  val output = Process("java", opts ++ Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) error("Unexpected output:\n" + output)
}

// for sbt scripted test:
TaskKey[Unit]("check") <<= (fullClasspath in Test in tracer, mainClass in Test in tracer, javaOptions in run in Test in tracer) map { (cp, mc, opts) =>
  val expected = "Printing sample:\nhello\n"
  val output = Process("java", opts ++ Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) error("Unexpected output:\n" + output)
}

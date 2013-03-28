// for sbt scripted test:
TaskKey[Unit]("check") <<= (fullClasspath in Runtime, mainClass in Runtime) map { (cp, mc) =>
  val expected = "Printing sample:\nhello\n"
  val output = Process("java", Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) error("Unexpected output:\n" + output)
}

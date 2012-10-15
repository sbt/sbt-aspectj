// for sbt scripted test:
TaskKey[Unit]("check") <<= (fullClasspath in Runtime, mainClass in Runtime) map { (cp, mc) =>
  val expected = "Actor asked world\nhello world\n"
  val output = Process("java", Seq("-classpath", cp.files.absString, mc getOrElse "")).!!
  if (output != expected) error("Unexpected output:\n" + output)
}

import sbt._

class AspectjPluginProject(info: ProjectInfo) extends PluginProject(info) {
  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  val aspectjTools = "org.aspectj" % "aspectjtools" % "1.6.11"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Typesafe Publish Repo" at "http://repo.typesafe.com/typesafe/maven-releases/"
  Credentials(Path.userHome / ".ivy2" / "typesafe-credentials", log)
}

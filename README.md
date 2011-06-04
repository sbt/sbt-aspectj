AspectJ sbt plugin
==================

Usage: 

<pre>
  class MyAspectJProject(info: ProjectInfo) extends DefaultProject(info) with AspectjProject {

    ...

    // AspectJ setup

    //override def showWeaveInfo = true
    //override def aspectjVerbose = true

    val akkaTargetJars = configurationPath(Configurations.Compile) ** ("akka-actor-*.jar" || "akka-remote-*.jar")

    val testsJars = configurationPath(Configurations.Compile) ** "*tests*.jar"
    val sourcesJars = configurationPath(Configurations.Compile) ** "*sources*.jar"
    val docsJars = configurationPath(Configurations.Compile) ** "*docs*.jar"

    val aspectjTargetJars = akkaTargetJars --- testsJars --- sourcesJars --- docsJars

    override def filterAspects(jar: Path, aspects: PathFinder): PathFinder = {
      if (jar.name.contains("akka-actor")) aspects ** "Actor*"
      else if (jar.name.contains("akka-remote")) aspects ** "Remote*"
      else Path.emptyPathFinder
    }

    // add ajc to task dependencies

    override def testCompileAction = super.testCompileAction dependsOn(ajc)
    override def packageAction = super.packageAction dependsOn(ajc)

    // alter the runtime classpath for all the ways that sbt accesses it, including inter-project dependencies

    def instrumentedJars = aspectjOutputPath ** "*.jar"
    def removeJars = aspectjTargetJars +++ sourcesJars +++ docsJars

    override def managedClasspath(config: Configuration) = config match {
      case Configurations.Runtime => super.managedClasspath(config) --- removeJars +++ instrumentedJars
      case otherConfig            => super.managedClasspath(otherConfig)
    }
  }
<pre>
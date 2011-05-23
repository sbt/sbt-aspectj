/**
 *  Copyright (C) 2011 Typesafe, Inc <http://typesafe.com>
 */

import sbt._
import org.aspectj.tools.ajc.Main

/**
 * SBT plugin for AspectJ compiler.
 */
trait AspectjProject extends DefaultProject {

  val AspectjVersion = "1.6.11"

  // User API

  val aspectjTargetJars: PathFinder

  // Overridable

  def aspectSourcePath = mainSourcePath / "aspects"
  def aspectjOutputPath = outputPath / "aspectj"
  def showWeaveInfo = false
  def aspectjVerbose = false
  def sourceLevel = "-1.5"

  def filterAspects(jar: Path, aspects: PathFinder): PathFinder = aspects

  def renameInstrumentedJar(jar: Path) = {
    val (base, ext) = jar.baseAndExt
    base + "-instrumented" + "." + ext
  }

  // Main ajc task

  def aspectSources = aspectSourcePath ** "*.aj"

  def productJars = aspectjTargetJars.get.map(renameInstrumentedJar).map(aspectjOutputPath / _)

  lazy val ajc = fileTask("ajc", productJars from aspectSources) {
    FileUtilities.createDirectory(aspectjOutputPath, log)
    aspectjTargetJars.get.foreach { jar =>
      val aspects = filterAspects(jar, aspectSources)
      val outputJar = aspectjOutputPath / renameInstrumentedJar(jar)
      if (aspects.get.isEmpty) {
        log.info("No aspects for %s" format jar)
        log.info("Copying jar to %s" format outputJar)
        FileUtilities.copyFile(jar, outputJar, log)
      } else {
        log.info("Weaving %s with aspects: %s \nto %s" format (jar, aspects, outputJar))
        val ajc = new Main
        ajc.runMain(ajcOptions(jar, aspects, outputJar), false)
      }
    }
    None
  } dependsOn (copyResources, compile) describedAs ("Run AspectJ compiler; ajc")

  // AspectJ configuration

  val aspectj = config("aspectj") hide
  val aspectjClasspath = managedClasspath(aspectj)

  // use the typesafe repo cache for dependencies

  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

  // AspectJ dependencies

  val aspectjTools = "org.aspectj" % "aspectjtools" % AspectjVersion % "aspectj"

  val aspectjRuntime = "org.aspectj" % "aspectjrt" % AspectjVersion

  // Implementation

  private def inPath(jar: Path) = (jar +++ mainCompilePath).absString

  private val ajcClasspath = aspectjClasspath +++ compileClasspath +++ mainDependencies.scalaLibrary

  private def ajcOptions(jar: Path, aspects: PathFinder, outputJar: Path) = {
    var options = List[String]()
    if (showWeaveInfo) options ::= "-showWeaveInfo"
    if (aspectjVerbose) options ::= "-verbose"
    options ::= "-classpath"
    options ::= ajcClasspath.absString
    options ::= "-inpath"
    options ::= inPath(jar)
    options ::= "-outjar"
    options ::= outputJar.absolutePath
    options ::= sourceLevel
    aspects.get.map(_.absolutePath).foreach(options ::= _)
    options.reverse.toArray
  }
}

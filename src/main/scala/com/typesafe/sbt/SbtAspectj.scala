package com.typesafe.sbt

import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.plugins.JvmPlugin

import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main

object SbtAspectj extends AutoPlugin {

  object autoImport extends AspectjKeys {
    val Aspectj = config("aspectj") hide
  }

  override def requires = JvmPlugin

  import autoImport._
  import Ajc._

  lazy val aspectjSettings: Seq[Setting[_]] = inConfig(Aspectj)(defaultAspectjSettings) ++ aspectjDependencySettings

  override def projectConfigurations: Seq[Configuration] = Seq(Aspectj)

  override def projectSettings: Seq[Def.Setting[_]] = aspectjSettings

  def defaultAspectjSettings = Seq(
    aspectjVersion := "1.8.10",
    aspectjDirectory := crossTarget.value / "aspectj",
    showWeaveInfo := false,
    verbose := false,
    compileOnly := false,
    outXml := compileOnly.value,
    sourceLevel := "-1.5",
    lintProperties := Seq.empty,
    lintPropertiesFile := writeLintProperties.value,
    extraAspectjOptions := Seq.empty,
    aspectjOptions := AspectjOptions(
      showWeaveInfo.value,
      verbose.value,
      compileOnly.value,
      outXml.value,
      sourceLevel.value,
      lintPropertiesFile.value,
      extraAspectjOptions.value
    ),
    aspectjSource := (sourceDirectory in Compile).value / "aspectj",
    sourceDirectories := Seq(aspectjSource.value),
    compiledClasses := (classDirectory in Compile).value,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources := collectAspectSources.value,
    classDirectory := aspectjDirectory.value / "classes",
    inputs := Seq.empty,
    binaries := Seq.empty,
    managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
    aspectjClasspath := combineClasspaths.value,
    ajc := ajcTask.value,
    copyResources := copyResourcesTask.dependsOn(copyResources in Compile).value,
    weave := ajc.dependsOn(copyResources).value,
    products := Seq(weave.value),
    weaver := getWeaver.value,
    weaverOptions := createWeaverOptions.value
  )

  def aspectjDependencySettings = Seq(
    ivyConfigurations += Aspectj,
    libraryDependencies ++= {
      val version = (aspectjVersion in Aspectj).value
      Seq(
        "org.aspectj" % "aspectjtools" % version % Aspectj,
        "org.aspectj" % "aspectjweaver" % version % Aspectj,
        "org.aspectj" % "aspectjrt" % version
      )
    }
  )

  object Ajc {
    def writeLintProperties = Def.task {
      val props = lintProperties.value
      if (props.nonEmpty) {
        val file = aspectjDirectory.value / "lint.properties"
        IO.writeLines(file, props)
        Some(file)
      } else None
    }

    def combineClasspaths = Def.task {
      Attributed.blank((classDirectory in Compile).value) +:
        (managedClasspath.value ++ (dependencyClasspath in Compile).value)
    }

    def collectAspectSources = Def.task {
      sourceDirectories.value.descendantsExcept(includeFilter.value, excludeFilter.value).get
    }

    def ajcTask = Def.task {
      val cacheDir  = streams.value.cacheDirectory / "aspectj"
      val outputDir = classDirectory.value

      val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ =>
        runAjc(inputs.value, sources.value, binaries.value, outputDir,
          aspectjOptions.value, aspectjClasspath.value.files, cacheDir, streams.value.log
        )
        Set(outputDir)
      }
      val expanded = (inputs.value ++ binaries.value) flatMap { i => if (i.isDirectory) (i ** "*.class").get else Seq(i) }
      val cacheInputs = (expanded ++ sources.value).toSet
      cached(cacheInputs)
      outputDir
    }

    def runAjc(inputs: Seq[File], sources: Seq[File], binaries: Seq[File], output: File, options: AspectjOptions, classpath: Seq[File], cacheDir: File, log: Logger): Unit = {
      val opts = ajcOptions(inputs, sources, binaries, output, options, classpath).toArray
      logCounts(inputs, sources, binaries, output, options, log)
      IO.createDirectory(output.getParentFile)
      runAjcMain(opts, log)
    }

    def ajcOptions(inputs: Seq[File], sources: Seq[File], binaries: Seq[File], output: File, options: AspectjOptions, classpath: Seq[File]): Seq[String] = {
      Seq(options.sourceLevel) ++
      flagOption("-showWeaveInfo", options.showWeaveInfo) ++
      flagOption("-verbose", options.verbose) ++
      flagOption("-XterminateAfterCompilation", options.compileOnly) ++
      flagOption("-outxml", options.outXml) ++
      fileOption("-Xlintfile", options.lintProperties) ++
      pathOption("-inpath", inputs) ++
      pathOption("-aspectpath", binaries) ++
      pathOption("-classpath", classpath) ++
      fileOption("-d", output) ++
      options.extraOptions ++
      sources.map(_.absolutePath)
    }

    def flagOption(option: String, enabled: Boolean): Seq[String] = {
      if (enabled) Seq(option) else Seq.empty
    }

    def pathOption(option: String, files: Seq[File]): Seq[String] = {
      if (files.nonEmpty) Seq(option, files.absString) else Seq.empty
    }

    def fileOption(option: String, file: File): Seq[String] = {
      Seq(option, file.absolutePath)
    }

    def fileOption(option: String, file: Option[File]): Seq[String] = {
      if (file.isDefined) Seq(option, file.get.absolutePath) else Seq.empty
    }

    def logCounts(inputs: Seq[File], sources: Seq[File], binaries: Seq[File], output: File, options: AspectjOptions, log: Logger): Unit = {
      def opt(s: String): Option[String] = if (s.isEmpty) None else Some(s)
      val running = if (options.compileOnly) "Compiling" else "Weaving"
      val inputCount = counted("input", "", "s", inputs.length)
      val sourceCount = counted("AspectJ source", "", "s", sources.length)
      val binaryCount = counted("AspectJ binar", "y", "ies", binaries.length)
      val aspectCount = opt((sourceCount ++ binaryCount).mkString(" and "))
      val counts = (inputCount ++ aspectCount).mkString(" with ")
      if (counts.nonEmpty) { log.info("%s %s to %s..." format (running, counts, output)) }
    }

    def counted(prefix: String, single: String, plural: String, count: Int): Option[String] = count match {
      case 0 => None
      case 1 => Some("1 " + prefix + single)
      case n => Some(n.toString + " " + prefix + plural)
    }

    def copyResourcesTask = Def.task {
      val classes = (classDirectory in Compile).value
      val mappings = (copyResources in Compile).value

      val cacheFile = streams.value.cacheDirectory / "aspectj" / "resource-sync"
      val mapped = if (inputs.value contains classes) {
        mappings map (_._2) pair rebase(classes, classDirectory.value)
      } else Seq.empty

      Sync(cacheFile)(mapped)
      mapped
    }

    def runAjcMain(options: Array[String], log: Logger): Unit = {
      log.debug("Running AspectJ compiler with:")
      log.debug("ajc " + options.mkString(" "))
      val ajc = new Main
      val handler = new MessageHandler
      val showWeaveInfo = options contains "-showWeaveInfo"
      val verbose = options contains "-verbose"
      val logger = new IMessageHandler {
        var errors = false
        def handleMessage(message: IMessage): Boolean = {
          import IMessage._
          message.getKind match {
            case WEAVEINFO       => if (showWeaveInfo) log.info(message.toString)
            case INFO            => if (verbose) log.info(message.toString)
            case DEBUG | TASKTAG => log.debug(message.toString)
            case WARNING         => log.warn(message.toString)
            case ERROR           => log.error(message.toString); errors = true
            case FAIL | ABORT    => throw new AbortException(message)
          }
          true
        }
        def isIgnoring(kind: IMessage.Kind) = false
        def dontIgnore(kind: IMessage.Kind) = ()
        def ignore(kind: IMessage.Kind) = ()
      }
      handler.setInterceptor(logger)
      ajc.setHolder(handler)
      ajc.runMain(options, false)
      if (logger.errors) throw new AbortException("AspectJ failed")
    }

    def getWeaver = Def.task {
      update.value.matching {
        moduleFilter(organization = "org.aspectj", name = "aspectjweaver") &&
          artifactFilter(`type` = "jar")
      }.headOption
    }

    def createWeaverOptions = Def.task {
      weaver.value.toSeq map { "-javaagent:" + _ }
    }
  }

  // helper methods

  def useInstrumentedClasses(config: Configuration) = Def.task {
    val cp   = (fullClasspath in config).value
    val ins  = (inputs in Aspectj).value
    val outs = (weave in Aspectj).value

    insertInstrumentedClasses(cp, ins, outs)
  }

  def insertInstrumentedClasses(classpath: Classpath, inputs: Seq[File], output: File) = {
    (classpath filterNot { inputs contains _.data }) :+ Attributed.blank(output)
  }
}

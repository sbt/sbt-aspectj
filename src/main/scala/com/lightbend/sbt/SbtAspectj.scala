package com.lightbend.sbt

import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.plugins.JvmPlugin

import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main

object SbtAspectj extends AutoPlugin {

  object autoImport extends AspectjKeys {
    val Aspectj = config("aspectj").hide
  }

  override def requires = JvmPlugin

  import autoImport._
  import Ajc._

  lazy val aspectjSettings: Seq[Setting[_]] = inConfig(Aspectj)(defaultAspectjSettings) ++ aspectjDependencySettings

  override def projectConfigurations: Seq[Configuration] = Seq(Aspectj)

  override def projectSettings: Seq[Def.Setting[_]] = aspectjSettings

  def defaultAspectjSettings = Seq(
    aspectjVersion := "1.9.6",
    aspectjDirectory := crossTarget.value / "aspectj",
    aspectjShowWeaveInfo := false,
    aspectjVerbose := false,
    aspectjCompileOnly := false,
    aspectjOutXml := aspectjCompileOnly.value,
    aspectjSourceLevel := "-1.9",
    aspectjLintProperties := Seq.empty,
    aspectjLintPropertiesFile := writeLintProperties.value,
    aspectjExtraOptions := Seq.empty,
    aspectjOptions := AspectjOptions(
      aspectjShowWeaveInfo.value,
      aspectjVerbose.value,
      aspectjCompileOnly.value,
      aspectjOutXml.value,
      aspectjSourceLevel.value,
      aspectjLintPropertiesFile.value,
      aspectjExtraOptions.value
    ),
    aspectjSource := (sourceDirectory in Compile).value / "aspectj",
    sourceDirectories := Seq(aspectjSource.value),
    aspectjCompiledClasses := compiledClassesTask.value,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources := collectAspectSources.value,
    classDirectory := aspectjDirectory.value / "classes",
    aspectjInputs := Seq.empty,
    aspectjBinaries := Seq.empty,
    managedClasspath := Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
    aspectjClasspath := combineClasspaths.value,
    ajc := ajcTask.value,
    copyResources := copyResourcesTask.value,
    aspectjWeave := ajc.dependsOn(copyResources).value,
    products := Seq(aspectjWeave.value),
    aspectjWeaver := getWeaver.value,
    aspectjWeaverOptions := createWeaverOptions.value
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
      val props = aspectjLintProperties.value
      val dir = aspectjDirectory.value
      if (props.nonEmpty) {
        val file = dir / "lint.properties"
        IO.writeLines(file, props)
        Some(file)
      } else None
    }

    def combineClasspaths = Def.task {
      Attributed.blank((classDirectory in Compile).value) +:
        (managedClasspath.value ++ (dependencyClasspath in Compile).value)
    }.dependsOn(compile in Compile)

    def collectAspectSources = Def.task {
      sourceDirectories.value.descendantsExcept(includeFilter.value, excludeFilter.value).get
    }

    def ajcTask = Def.task {
      val cacheDir  = streams.value.cacheDirectory / "aspectj"
      val outputDir = classDirectory.value
      val inputs = aspectjInputs.value
      val ajcSources = sources.value
      val binaries = aspectjBinaries.value
      val options = aspectjOptions.value
      val classpath = aspectjClasspath.value.files
      val log = streams.value.log

      val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ =>
        runAjc(inputs, ajcSources, binaries, outputDir, options, classpath, cacheDir, log)
        Set(outputDir)
      }
      val expanded = (inputs ++ binaries) flatMap { i => if (i.isDirectory) (i ** "*.class").get else Seq(i) }
      val cacheInputs = (expanded ++ ajcSources).toSet
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
      val inputs = aspectjInputs.value
      val compileClassDir = (classDirectory in Compile).value
      val resourceMappings = (copyResources in Compile).value
      val aspectjClassDir = classDirectory.value
      val taskStreams = streams.value
      SbtAspectjExtra.copyResources(inputs, compileClassDir, resourceMappings, aspectjClassDir, taskStreams)
    }

    def runAjcMain(options: Array[String], log: Logger): Unit = {
      log.debug("Running AspectJ compiler with:")
      log.debug("ajc " + options.mkString(" "))
      val ajc = new Main
      val handler = new MessageHandler
      val showWeaveInfo = options contains "-showWeaveInfo"
      val verbose = options contains "-verbose"
      var errors = false
      val logger = new IMessageHandler {
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
      if (errors) throw new AbortException("AspectJ failed")
    }

    def getWeaver = Def.task {
      update.value.matching {
        moduleFilter(organization = "org.aspectj", name = "aspectjweaver") &&
          artifactFilter(`type` = "jar")
      }.headOption
    }

    def createWeaverOptions = Def.task {
      aspectjWeaver.value.toSeq map { "-javaagent:" + _ }
    }
  }

  // helper methods

  private def compiledClassesTask = Def.task {
    (classDirectory in Compile).value
  }.dependsOn(compile in Compile)

  def aspectjUseInstrumentedClasses(config: Configuration) = Def.task {
    val cp   = (fullClasspath in config).value
    val ins  = (aspectjInputs in Aspectj).value
    val outs = (aspectjWeave in Aspectj).value

    aspectjInsertInstrumentedClasses(cp, ins, outs)
  }

  def aspectjInsertInstrumentedClasses(classpath: Classpath, inputs: Seq[File], output: File) = {
    (classpath filterNot { inputs contains _.data }) :+ Attributed.blank(output)
  }
}

package com.typesafe.sbt

import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.Project.Initialize

import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main

object SbtAspectj extends Plugin {
  case class AspectjOptions(
    showWeaveInfo: Boolean,
    verbose: Boolean,
    compileOnly: Boolean,
    outXml: Boolean,
    sourceLevel: String,
    lintProperties: Option[File],
    extraOptions: Seq[String]
  )

  val Aspectj = config("aspectj") hide

  object AspectjKeys {
    val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")

    val aspectjDirectory = SettingKey[File]("aspectj-directory", "Target directory for AspectJ.")

    val showWeaveInfo = SettingKey[Boolean]("show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
    val verbose = SettingKey[Boolean]("verbose", "Enable the -verbose AspectJ option.")
    val compileOnly = SettingKey[Boolean]("compile-only", "The AspectJ -XterminateAfterCompilation option.")
    val outXml = SettingKey[Boolean]("out-xml", "Enable the -outxml AspectJ option.")
    val sourceLevel = SettingKey[String]("source-level", "The AspectJ source level option.")
    val lintProperties = SettingKey[Seq[String]]("lint-properties", "AspectJ -Xlint properties.")
    val lintPropertiesFile = TaskKey[Option[File]]("lint-properties-file", "Write any -Xlint properties to a file.")
    val extraAspectjOptions = TaskKey[Seq[String]]("extra-aspectj-options", "Extra AspectJ options (which don't have provided sbt settings).")
    val aspectjOptions = TaskKey[AspectjOptions]("aspectj-options", "Configurable AspectJ options.")

    val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
    val inputs = TaskKey[Seq[File]]("inputs", "Jars or class directories to weave. Passed to the -inpath AspectJ option.")
    val binaries = TaskKey[Seq[File]]("binaries", "Binary aspects passed to the -aspectpath AspectJ option.")
    val output = TaskKey[File]("output", "The output class directory or jar file for AspectJ.")
    val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath", "The classpath used for running AspectJ.")

    val ajc = TaskKey[File]("ajc", "Run the AspectJ compiler.")
    val weave = TaskKey[File]("weave", "Weave with AspectJ.")

    val weaver = TaskKey[Option[File]]("weaver", "Location of AspectJ load-time weaver.")
    val weaverOptions = TaskKey[Seq[String]]("weaver-options", "JVM options for AspectJ weaver java agent.")
  }

  import AspectjKeys._
  import Ajc._

  lazy val aspectjSettings: Seq[Setting[_]] = inConfig(Aspectj)(defaultAspectjSettings) ++ aspectjDependencySettings

  def defaultAspectjSettings = Seq(
    aspectjVersion := "1.8.5",
    aspectjDirectory <<= crossTarget / "aspectj",
    showWeaveInfo := false,
    verbose := false,
    compileOnly := false,
    outXml <<= compileOnly,
    sourceLevel := "-1.5",
    lintProperties := Seq.empty,
    lintPropertiesFile <<= writeLintProperties,
    extraAspectjOptions := Seq.empty,
    aspectjOptions <<= (showWeaveInfo, verbose, compileOnly, outXml, sourceLevel, lintPropertiesFile, extraAspectjOptions) map AspectjOptions,
    aspectjSource <<= (sourceDirectory in Compile) / "aspectj",
    sourceDirectories <<= Seq(aspectjSource).join,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources <<= collectAspectSources,
    classDirectory <<= aspectjDirectory / "classes",
    inputs := Seq.empty,
    binaries := Seq.empty,
    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    aspectjClasspath <<= combineClasspaths,
    ajc <<= ajcTask,
    copyResources <<= copyResourcesTask,
    weave <<= (ajc, copyResources) map { (instrumented, _) => instrumented },
    products <<= weave map { instrumented => Seq(instrumented) },
    weaver <<= getWeaver,
    weaverOptions <<= createWeaverOptions
  )

  def aspectjDependencySettings = Seq(
    ivyConfigurations += Aspectj,
    libraryDependencies <++= (aspectjVersion in Aspectj) { version => Seq(
      "org.aspectj" % "aspectjtools" % version % Aspectj.name,
      "org.aspectj" % "aspectjweaver" % version % Aspectj.name,
      "org.aspectj" % "aspectjrt" % version
    )}
  )

  object Ajc {
    def writeLintProperties = (lintProperties, aspectjDirectory) map { (props, dir) =>
      if (props.nonEmpty) {
        val file = dir / "lint.properties"
        IO.writeLines(file, props)
        Some(file)
      } else None
    }

    def combineClasspaths = (managedClasspath, dependencyClasspath in Compile, compile in Compile, classDirectory in Compile) map {
      (mcp, dcp, _, classes) => Attributed.blank(classes) +: (mcp ++ dcp)
    }

    def collectAspectSources = (sourceDirectories, includeFilter, excludeFilter) map {
      (dirs, include, exclude) => dirs.descendantsExcept(include, exclude).get
    }

    def ajcTask = (inputs, sources, binaries, classDirectory, aspectjOptions, aspectjClasspath, cacheDirectory, streams) map {
      (inputs, sources, binaries, output, options, classpath, cache, s) => {
        val cacheDir = cache / "aspectj"
        val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ =>
          runAjc(inputs, sources, binaries, output, options, classpath.files, cacheDir, s.log)
          Set(output)
        }
        val expanded = (inputs ++ binaries) flatMap { i => if (i.isDirectory) (i ** "*.class").get else Seq(i) }
        val cacheInputs = (expanded ++ sources).toSet
        cached(cacheInputs)
        output
      }
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

    def copyResourcesTask = (cacheDirectory, inputs, classDirectory in Compile, copyResources in Compile, classDirectory) map {
      (cache, inputs, classes, mappings, output) => {
        val cacheFile = cache / "aspectj" / "resource-sync"
        val mapped = if (inputs contains classes) { mappings map (_._2) x rebase(classes, output) } else Seq.empty
        Sync(cacheFile)(mapped)
        mapped
      }
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

    def getWeaver = update map { report =>
      report.matching(moduleFilter(organization = "org.aspectj", name = "aspectjweaver")).headOption
    }

    def createWeaverOptions = weaver map { weaver =>
      weaver.toSeq map { "-javaagent:" + _ }
    }
  }

  // helper methods

  def compiledClasses = (compile in Compile, classDirectory in Compile) map {
    (_, classes) => classes
  }

  def useInstrumentedClasses(config: Configuration) = {
    (fullClasspath in config, inputs in Aspectj, weave in Aspectj) map {
      (cp, inputs, output) => insertInstrumentedClasses(cp, inputs, output)
    }
  }

  def insertInstrumentedClasses(classpath: Classpath, inputs: Seq[File], output: File) = {
    (classpath filterNot { inputs contains _.data }) :+ Attributed.blank(output)
  }
}

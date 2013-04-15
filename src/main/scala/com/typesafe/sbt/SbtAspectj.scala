package com.typesafe.sbt

import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.Project.Initialize

import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main

object SbtAspectj extends Plugin {
  val Aspectj = config("aspectj") hide

  object AspectjKeys {
    val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")

    val showWeaveInfo = SettingKey[Boolean]("show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
    val verbose = SettingKey[Boolean]("verbose", "Enable the -verbose AspectJ option.")
    val sourceLevel = SettingKey[String]("source-level", "The AspectJ source level option.")
    val outXml = SettingKey[Boolean]("out-xml", "Enable the -outxml AspectJ option.")

    val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
    val inputs = TaskKey[Seq[File]]("inputs", "Jars or class directories to weave. Passed to the -inpath AspectJ option.")
    val binaries = TaskKey[Seq[File]]("binaries", "Binary aspects passed to the -aspectpath AspectJ option.")
    val output = TaskKey[File]("output", "The output class directory or jar file for AspectJ.")
    val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath", "The classpath used for running AspectJ.")
    val baseOptions = TaskKey[Seq[String]]("base-options", "The showWeaveInfo, verbose, and sourceLevel settings as options.")

    val compiledClasses = TaskKey[File]("compiled-classes", "The compile classes directory (after compile).")

    val ajc = TaskKey[File]("ajc", "Run the AspectJ compiler.")
    val weave = TaskKey[File]("weave", "Weave with AspectJ.")

    // load-time weaving support - compiling and including aspects in package-bin
    val aspectClassesDirectory = SettingKey[File]("aspect-classes-directory")
    val compileAspects = TaskKey[File]("compile-aspects", "Compile aspects for load-time weaving.")
    val enableProducts = TaskKey[Boolean]("enableProducts", "Enable or disable compiled aspects in compile products.")
    val aspectProducts = TaskKey[Seq[File]]("aspect-products", "Optionally compiled aspects (if produce-aspects).")
    val weaveAgentJar = TaskKey[Option[File]]("weave-agent-jar", "Location of AspectJ weaver.")
    val weaveAgentOptions = TaskKey[Seq[String]]("weave-agent-options", "JVM options for AspectJ java agent.")
  }

  import AspectjKeys._

  lazy val aspectjSettings: Seq[Setting[_]] = inConfig(Aspectj)(baseAspectjSettings) ++ dependencySettings

  def baseAspectjSettings = Seq(
    aspectjVersion := "1.7.2",
    showWeaveInfo := false,
    verbose := false,
    sourceLevel := "-1.5",
    outXml := true,
    aspectjSource <<= (sourceDirectory in Compile) / "aspectj",
    sourceDirectories <<= Seq(aspectjSource).join,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources <<= collectAspectSources,
    classDirectory <<= crossTarget { _ / "aspectj" / "classes" },
    inputs := Seq.empty,
    binaries := Seq.empty,
    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    dependencyClasspath <<= dependencyClasspath in Compile,
    compiledClasses <<= compileClasses,
    aspectjClasspath <<= combineClasspaths,
    baseOptions <<= ajcBaseOptions,
    ajc <<= ajcTask,
    copyResources <<= copyResourcesTask,
    weave <<= (ajc, copyResources) map { (instrumented, _) => instrumented },
    products <<= weave map { instrumented => Seq(instrumented) },
    compileAspects <<= compileAspectsTask,
    enableProducts := false,
    aspectProducts <<= compileIfEnabled,
    products in Compile <<= combineProducts,
    weaveAgentJar <<= javaAgent,
    weaveAgentOptions <<= javaAgentOptions
  )

  def dependencySettings = Seq(
    ivyConfigurations += Aspectj,
    libraryDependencies <++= (aspectjVersion in Aspectj) { version => Seq(
      "org.aspectj" % "aspectjtools" % version % Aspectj.name,
      "org.aspectj" % "aspectjweaver" % version % Aspectj.name,
      "org.aspectj" % "aspectjrt" % version
    )}
  )

  def compileClasses = (compile in Compile, compileInputs in Compile) map {
    (_, inputs) => inputs.config.classesDirectory
  }

  def combineClasspaths = (managedClasspath, dependencyClasspath, compiledClasses) map {
    (mcp, dcp, classes) => Attributed.blank(classes) +: (mcp ++ dcp)
  }

  def ajcBaseOptions = (showWeaveInfo, verbose, sourceLevel) map {
    (info, verbose, level) => {
      (if (info) Seq("-showWeaveInfo") else Seq.empty[String]) ++
      (if (verbose) Seq("-verbose") else Seq.empty[String]) ++
      Seq(level)
    }
  }

  def collectAspectSources = (sourceDirectories, includeFilter, excludeFilter) map {
    (dirs, include, exclude) => dirs.descendantsExcept(include, exclude).get
  }

  def ajcTask = (cacheDirectory, inputs, sources, binaries, classDirectory, baseOptions, aspectjClasspath, streams) map {
    (cache, inputs, sources, binaries, output, baseOpts, cp, s) => {
      val cacheDir = cache / "aspectj"
      val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ =>
        val options = baseOpts ++ Seq("-classpath", cp.files.absString)
        runAjc(inputs, sources, binaries, output, options, cacheDir, s.log)
        Set(output)
      }
      val allInputs = inputs flatMap { i => if (i.isDirectory) (i ** "*.class").get else Seq(i) }
      val cacheInputs = (allInputs ++ sources ++ binaries).toSet
      cached(cacheInputs)
      output
    }
  }

  def runAjc(inputs: Seq[File], sources: Seq[File], binaries: Seq[File], output: File, baseOptions: Seq[String], cacheDir: File, log: Logger): Unit = {
    IO.createDirectory(output.getParentFile)
    val options = ajcOptions(inputs, sources, binaries, output, baseOptions).toArray
    ajcRunMain(options, log)
  }

  def ajcOptions(inputs: Seq[File], sources: Seq[File], binaries: Seq[File], output: File, baseOptions: Seq[String]): Seq[String] = {
    baseOptions ++
    { if (inputs.nonEmpty) Seq("-inpath", inputs.absString) else Seq.empty } ++
    { if (binaries.nonEmpty) Seq("-aspectpath", binaries.absString) else Seq.empty } ++
    Seq("-d", output.absolutePath) ++
    sources.map(_.absolutePath)
  }

  def copyResourcesTask = (cacheDirectory, inputs, classDirectory in Compile, copyResources in Compile, classDirectory) map {
    (cache, inputs, classes, mappings, output) => {
      val cacheFile = cache / "aspectj" / "resource-sync"
      val mapped = if (inputs contains classes) { mappings map (_._2) x rebase(classes, output) } else Seq.empty
      Sync(cacheFile)(mapped)
      mapped
    }
  }

  def ajcRunMain(options: Array[String], log: Logger): Unit = {
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

  def useInstrumentedClasses(config: Configuration) = {
    (fullClasspath in config, inputs in Aspectj, weave in Aspectj) map {
      (cp, inputs, output) => insertInstrumentedClasses(cp, inputs, output)
    }
  }

  def insertInstrumentedClasses(classpath: Classpath, inputs: Seq[File], output: File) = {
    classpath map { a => if (inputs contains a.data) Attributed.blank(output) else a }
  }

  def compileAspectsTask = (sources, outXml, aspectjClasspath, baseOptions, classDirectory, cacheDirectory, streams) map {
    (aspects, outXml, classpath, opts, dir, cacheDir, s) => {
      val cachedCompile = FileFunction.cached(cacheDir / "ajc-compile", FilesInfo.hash) { _ =>
        val sourceCount = Util.counted("AspectJ source", "", "s", aspects.length)
        sourceCount foreach { count => s.log.info("Compiling %s to %s..." format (count, dir)) }
        val options = ajcCompileOptions(aspects, outXml, classpath, opts, dir).toArray
        ajcRunMain(options, s.log)
        dir.***.get.toSet
      }
      val inputs = aspects.toSet
      cachedCompile(inputs)
      dir
    }
  }

  def ajcCompileOptions(aspects: Seq[File], outXml: Boolean, classpath: Classpath, baseOptions: Seq[String], output: File): Seq[String] = {
    baseOptions ++
    Seq("-XterminateAfterCompilation") ++
    Seq("-classpath", classpath.files.absString) ++
    Seq("-d", output.absolutePath) ++
    (if (outXml) Seq("-outxml") else Seq.empty[String]) ++
    aspects.map(_.absolutePath)
  }

  def compileIfEnabled = (enableProducts, compileAspects.task) flatMap {
    (enable, compile) => if (enable) (compile map { dir => Seq(dir) }) else task { Seq.empty[File] }
  }

  def combineProducts = (products in Compile, aspectProducts) map { _ ++ _ }

  def javaAgent = update map { report =>
    report.matching(moduleFilter(organization = "org.aspectj", name = "aspectjweaver")) headOption
  }

  def javaAgentOptions = weaveAgentJar map { weaver => weaver.toSeq map { "-javaagent:" + _ } }
}

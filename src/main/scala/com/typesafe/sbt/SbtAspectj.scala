package com.typesafe.sbt

import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.Project.Initialize

import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main

object SbtAspectj extends Plugin {
  case class Mapping(in: File, aspects: Seq[File], out: File)

  val Aspectj = config("aspectj") hide

  object AspectjKeys {
    val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")
    val showWeaveInfo = SettingKey[Boolean]("show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
    val verbose = SettingKey[Boolean]("verbose", "Enable the -verbose AspectJ option.")
    val sourceLevel = SettingKey[String]("source-level", "The AspectJ source level option.")
    val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
    val outputDirectory = SettingKey[File]("output-directory", "Output directory for AspectJ instrumentation.")
    val aspectFilter = SettingKey[(File, Seq[File]) => Seq[File]]("aspect-filter", "Filter for aspects. Used to create aspect mappings.")

    val compiledClasses = TaskKey[File]("compiled-classes", "The compile classes directory (after compile).")
    val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath", "The classpath used for running AspectJ.")
    val baseOptions = TaskKey[Seq[String]]("base-options", "The showWeaveInfo, verbose, and sourceLevel settings as options.")
    val inputs = TaskKey[Seq[File]]("inputs", "The jars or classes directories to weave.")
    val aspectMappings = TaskKey[Seq[Mapping]]("aspect-mappings", "Mappings from inputs, through aspects, to outputs.")

    val ajc = TaskKey[Seq[File]]("ajc", "Run the AspectJ compiler.")
    val weave = TaskKey[Seq[File]]("weave", "Weave with AspectJ.")

    // load-time weaving support - compiling and including aspects in package-bin
    val aspectClassesDirectory = SettingKey[File]("aspect-classes-directory")
    val outxml = SettingKey[Boolean]("outxml")
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
    aspectjSource <<= (sourceDirectory in Compile) / "aspectj",
    sourceDirectories <<= Seq(aspectjSource).join,
    outputDirectory <<= crossTarget / "aspectj",
    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    dependencyClasspath <<= dependencyClasspath in Compile,
    compiledClasses <<= compileClasses,
    aspectjClasspath <<= combineClasspaths,
    baseOptions <<= ajcBaseOptions,
    inputs := Seq.empty,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources <<= collectAspects,
    aspectFilter := { (f, a) => a },
    aspectMappings <<= mapAspects,
    ajc <<= ajcTask,
    copyResources <<= copyResourcesTask,
    weave <<= (ajc, copyResources) map { (instrumented, _) => instrumented },
    aspectClassesDirectory <<= outputDirectory / "classes",
    outxml := true,
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

  def collectAspects = (sourceDirectories, includeFilter, excludeFilter) map {
    (dirs, include, exclude) => dirs.descendantsExcept(include, exclude).get
  }

  def mapAspects = (inputs, sources, aspectFilter, outputDirectory) map {
    (in, aspects, filter, dir) => {
      in map { input => Mapping(input, filter(input, aspects), instrumented(input, dir)) }
    }
  }

  def instrumented(input: File, outputDir: File): File = {
    val (base, ext) = input.baseAndExt
    val outputName = {
      if (ext.isEmpty) base + "-instrumented"
      else base + "-instrumented" + "." + ext
    }
    outputDir / outputName
  }

  def ajcTask = (cacheDirectory, aspectMappings, baseOptions, aspectjClasspath, streams) map {
    (cache, mappings, options, cp, s) => {
      val cacheDir = cache / "aspectj"
      val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ =>
        val withPrevious = mappings.zipWithIndex map { case (m, i) => (mappings.take(i), m) }
        (withPrevious map { case (previousMappings, mapping) =>
          val classpath = insertInstrumentedJars(cp, previousMappings)
          val classpathOption = Seq("-classpath", classpath.files.absString)
          runAjc(mapping.in, mapping.aspects, mapping.out, options ++ classpathOption, cacheDir, s.log)
          mapping.out
        }).toSet
      }
      val cacheInputs = mappings.flatMap( mapping => {
        val input = mapping.in
        if (input.isDirectory) (input ** "*.class").get ++ mapping.aspects
        else input +: mapping.aspects
       }).toSet
      cached(cacheInputs).toSeq
    }
  }

  def runAjc(input: File, aspects: Seq[File], output: File, baseOptions: Seq[String], cacheDir: File, log: Logger): Unit = {
    IO.createDirectory(output.getParentFile)
    if (aspects.isEmpty) {
      log.info("No aspects for %s" format input)
      if (input.isDirectory) {
        log.info("Copying classes to %s" format output)
        val classes = (input ** "*.class") x rebase(input, output)
        Sync(cacheDir / "ajc-sync")(classes)
      } else {
        log.info("Copying jar to %s" format output)
        IO.copyFile(input, output, false)
      }
    } else {
      log.info("Weaving %s with aspects:" format input)
      aspects foreach { f => log.info("  " + f.absolutePath) }
      log.info("to %s" format output)
      val options = ajcOptions(input, aspects, output, baseOptions).toArray
      ajcRunMain(options, log)
    }
  }

  def ajcOptions(in: File, aspects: Seq[File], out: File, baseOptions: Seq[String]): Seq[String] = {
    baseOptions ++
    Seq("-inpath", in.absolutePath) ++
    { if (in.isDirectory) Seq("-d", out.absolutePath) else Seq("-outjar", out.absolutePath) } ++
    aspects.map(_.absolutePath)
  }

  def copyResourcesTask = (cacheDirectory, aspectMappings, copyResources in Compile) map {
    (cache, mappings, resourceMappings) => {
      val cacheFile = cache / "aspectj" / "resource-sync"
      val mapped = mappings flatMap { mapping =>
        if (mapping.in.isDirectory) {
          resourceMappings map (_._2) x rebase(mapping.in, mapping.out)
        } else Seq.empty
      }
      Sync(cacheFile)(mapped)
      mapped
    }
  }

  def ajcRunMain(options: Array[String], log: Logger): Unit = {
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

  def useInstrumentedJars(config: Configuration) = useInstrumentedClasses(config)
  def insertInstrumentedJars(classpath: Classpath, mappings: Seq[Mapping]) = insertInstrumentedClasses(classpath, mappings)

  def useInstrumentedClasses(config: Configuration) = {
    (fullClasspath in config, aspectMappings in Aspectj, weave in Aspectj) map {
      (cp, mappings, woven) => insertInstrumentedClasses(cp, mappings)
    }
  }

  def insertInstrumentedClasses(classpath: Classpath, mappings: Seq[Mapping]) = {
    classpath map { a => mappings.find(_.in == a.data).map(_.out).map(Attributed.blank).getOrElse(a) }
  }

  def compileAspectsTask = (sources, outxml, aspectjClasspath, baseOptions, aspectClassesDirectory, cacheDirectory, streams) map {
    (aspects, outxml, classpath, opts, dir, cacheDir, s) => {
      val cachedCompile = FileFunction.cached(cacheDir / "ajc-compile", FilesInfo.hash) { _ =>
        val sourceCount = Util.counted("AspectJ source", "", "s", aspects.length)
        sourceCount foreach { count => s.log.info("Compiling %s to %s..." format (count, dir)) }
        val options = ajcCompileOptions(aspects, outxml, classpath, opts, dir).toArray
        ajcRunMain(options, s.log)
        dir.***.get.toSet
      }
      val inputs = aspects.toSet
      cachedCompile(inputs)
      dir
    }
  }

  def ajcCompileOptions(aspects: Seq[File], outxml: Boolean, classpath: Classpath, baseOptions: Seq[String], output: File): Seq[String] = {
    baseOptions ++
    Seq("-XterminateAfterCompilation") ++
    Seq("-classpath", classpath.files.absString) ++
    Seq("-d", output.absolutePath) ++
    (if (outxml) Seq("-outxml") else Seq.empty[String]) ++
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

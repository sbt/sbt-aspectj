package com.typesafe.sbt

import java.io.File
import sbt._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.Project.Initialize

object SbtAspectj extends Plugin {
  case class Mapping(in: File, aspects: Seq[File], out: File)

  val Aspectj = config("aspectj") hide

  object AspectjKeys {
    val aspectjVersion = SettingKey[String]("aspectj-version")
    val showWeaveInfo = SettingKey[Boolean]("show-weave-info")
    val verbose = SettingKey[Boolean]("verbose")
    val sourceLevel = SettingKey[String]("source-level")
    val aspectjSource = SettingKey[File]("aspectj-source")
    val outputDirectory = SettingKey[File]("output-directory")
    val aspectFilter = SettingKey[(File, Seq[File]) => Seq[File]]("aspect-filter")

    val compiledClasses = TaskKey[File]("compiled-classes")
    val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath")
    val baseOptions = TaskKey[Seq[String]]("base-options")
    val inputs = TaskKey[Seq[File]]("inputs")
    val aspectMappings = TaskKey[Seq[Mapping]]("aspect-mappings")

    val ajc = TaskKey[Seq[File]]("ajc", "Run the AspectJ compiler.")
    val weave = TaskKey[Seq[File]]("weave", "Weave with AspectJ.")

    // load-time weaving support - compiling and including aspects in package-bin
    val aspectClassesDirectory = SettingKey[File]("aspect-classes-directory")
    val compileAspects = TaskKey[File]("compile-aspects", "Compile aspects for load-time weaving.")
    val enableProducts = TaskKey[Boolean]("enableProducts", "Enable or disable compiled aspects in compile products.")
    val aspectProducts = TaskKey[Seq[File]]("aspect-products", "Optionally compiled aspects (if produce-aspects).")
    val weaveAgentOptions = TaskKey[Seq[String]]("weave-agent-options", "JVM options for AspectJ java agent.")
  }

  import AspectjKeys._

  lazy val aspectjSettings: Seq[Setting[_]] = inConfig(Aspectj)(baseAspectjSettings) ++ dependencySettings

  def baseAspectjSettings = Seq(
    aspectjVersion := "1.7.1",
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
    compileAspects <<= compileAspectsTask,
    enableProducts := false,
    aspectProducts <<= compileIfEnabled,
    products in Compile <<= combineProducts,
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
      val ajc = new org.aspectj.tools.ajc.Main
      val options = ajcOptions(input, aspects, output, baseOptions).toArray
      ajc.runMain(options, false)
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

  def compileAspectsTask = (sources, aspectjClasspath, baseOptions, aspectClassesDirectory, cacheDirectory, streams) map {
    (aspects, classpath, opts, dir, cacheDir, s) => {
      val cachedCompile = FileFunction.cached(cacheDir / "ajc-compile", FilesInfo.hash) { _ =>
        val sourceCount = Util.counted("AspectJ source", "", "s", aspects.length)
        sourceCount foreach { count => s.log.info("Compiling %s to %s..." format (count, dir)) }
        val options = ajcCompileOptions(aspects, classpath, opts, dir).toArray
        val ajc = new org.aspectj.tools.ajc.Main
        ajc.runMain(options, false)
        dir.***.get.toSet
      }
      val inputs = aspects.toSet
      cachedCompile(inputs)
      dir
    }
  }

  def ajcCompileOptions(aspects: Seq[File], classpath: Classpath, baseOptions: Seq[String], output: File): Seq[String] = {
    baseOptions ++
    Seq("-classpath", classpath.files.absString) ++
    Seq("-XterminateAfterCompilation", "-outxml") ++
    Seq("-d", output.absolutePath) ++
    aspects.map(_.absolutePath)
  }

  def compileIfEnabled = (enableProducts, compileAspects.task) flatMap {
    (enable, compile) => if (enable) (compile map { dir => Seq(dir) }) else task { Seq.empty[File] }
  }

  def combineProducts = (products in Compile, aspectProducts) map { _ ++ _ }

  def javaAgentOptions = update map { report =>
    report.matching(moduleFilter(organization = "org.aspectj", name = "aspectjweaver")) take 1 map { "-javaagent:" + _ }
  }
}

package com.typesafe.sbtaspectj

import sbt._
import Configurations.Compile
import Keys._

import java.io.File

object AspectjPlugin {
  case class Mapping(in: File, aspects: Seq[File], out: File)

  val Aspectj = config("aspectj") hide

  val aspectjVersion = SettingKey[String]("aspectj-version")
  val aspectjDirectory = SettingKey[File]("aspectj-directory")
  val outputDirectory = SettingKey[File]("output-directory")

  val showWeaveInfo = SettingKey[Boolean]("show-weave-info")
  val verbose = SettingKey[Boolean]("verbose")
  val sourceLevel = SettingKey[String]("source-level")

  val aspectFilter = SettingKey[(File, Seq[File]) => Seq[File]]("aspect-filter")

  val compiledClasses = TaskKey[File]("compiled-classes")
  val aspectjClasspath = TaskKey[Classpath]("aspectj-classpath")
  val baseOptions = TaskKey[Seq[String]]("base-options")

  val inputs = TaskKey[Seq[File]]("inputs")
  val sources = TaskKey[Seq[File]]("sources")
  val aspectMappings = TaskKey[Seq[Mapping]]("aspect-mappings")

  val ajc = TaskKey[Seq[File]]("ajc", "Run the AspectJ compiler.")
  val weave = TaskKey[Seq[File]]("weave", "Weave with Aspectj.")

  lazy val settings: Seq[Setting[_]] = inConfig(Aspectj)(aspectjSettings) ++ dependencySettings

  def aspectjSettings = Seq(
    aspectjVersion := "1.6.12",
    aspectjDirectory <<= sourceDirectory(_ / "main" / "aspectj"),
    outputDirectory <<= crossTarget / "aspectj",
    showWeaveInfo := false,
    verbose := false,
    sourceLevel := "-1.5",
    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    dependencyClasspath <<= dependencyClasspath in Compile,
    compiledClasses <<= (compile in Compile, compileInputs in Compile) map {
      (_, inputs) => inputs.config.classesDirectory
    },
    aspectjClasspath <<= (managedClasspath, dependencyClasspath, compiledClasses) map {
      (mcp, dcp, classes) => Attributed.blank(classes) +: (mcp ++ dcp)
    },
    baseOptions <<= ajcBaseOptions,
    inputs := Seq.empty,
    sources <<= aspectjDirectory map { dir => (dir ** "*.aj").get },
    aspectFilter := { (f, a) => a },
    aspectMappings <<= mapAspects,
    ajc <<= ajcTask,
    copyResources <<= copyResourcesTask,
    weave <<= (ajc, copyResources) map { (instrumented, _) => instrumented }
  )

  def dependencySettings = Seq(
    ivyConfigurations += Aspectj,
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies <+= (aspectjVersion in Aspectj)("org.aspectj" % "aspectjtools" % _ % Aspectj.name),
    libraryDependencies <+= (aspectjVersion in Aspectj)("org.aspectj" % "aspectjrt" % _))

  def ajcBaseOptions = (showWeaveInfo, verbose, sourceLevel) map {
    (info, verbose, level) => {
      (if (info) Seq("-showWeaveInfo") else Seq.empty[String]) ++
      (if (verbose) Seq("-verbose") else Seq.empty[String]) ++
      Seq(level)
    }
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
}

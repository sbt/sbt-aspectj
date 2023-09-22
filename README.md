sbt-aspectj
===========

[sbt] plugin for weaving with [aspectj]. Forked from https://github.com/sbt/sbt-aspectj, this plugin supports aspectj 1.9.6.

Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

    addSbtPlugin("com.liyutech" % "sbt-aspectj" % "{version}")



Example build.sbt:
    
```
    import sbtassembly.MergeStrategy
    import com.lightbend.sbt.SbtAspectj
    import com.lightbend.sbt.SbtAspectj.autoImport.aspectjInputs

    val Http4sVersion = "0.23.1"
    val CirceVersion = "0.14.1"
    val MunitVersion = "0.7.27"
    val LogbackVersion = "1.2.5"
    val MunitCatsEffectVersion = "1.0.5"
    val QuillVersion = "3.10.0"

    val EntryPoint = Some("com.liyutech.iot.smarthome.SmartHomeMain")

    val root = (project in file("."))
      .enablePlugins(SbtAspectj)
      .settings(
        organization := "com.liyutech.iot",
        name := "smarthome",
        version := "0.0.1-SNAPSHOT",
        scalaVersion := "2.13.6",
        libraryDependencies ++= Seq(
          "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
          "org.http4s" %% "http4s-ember-server" % Http4sVersion,
          "org.http4s" %% "http4s-ember-client" % Http4sVersion,
          "org.http4s" %% "http4s-circe" % Http4sVersion,
          "org.http4s" %% "http4s-dsl" % Http4sVersion,
          "io.circe" %% "circe-generic" % CirceVersion,
          "com.typesafe" % "config" % "1.4.1",
          "co.fs2" %% "fs2-reactive-streams" % "3.1.5",
          "org.typelevel" %% "cats-effect" % "3.2.3",
          "javax.media" % "jmf" % "2.1.1e",
          "org.aspectj" % "aspectjweaver" % "1.9.6",
          "io.getquill" %% "quill-jdbc" % QuillVersion,
          "io.getquill" %% "quill-jdbc-zio" % QuillVersion,
          "io.getquill" %% "quill-jasync-postgres" % QuillVersion,
          "io.getquill" %% "quill-async-postgres" % QuillVersion,
          "com.h2database" % "h2" % "1.4.199",
          "org.scalameta" %% "munit" % MunitVersion % Test,
          "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
          "ch.qos.logback" % "logback-classic" % LogbackVersion,
          "org.scalameta" %% "svm-subs" % "20.2.0",
          "org.flywaydb" % "flyway-core" % "8.0.4"
        ),
        javacOptions ++= Seq("-source", "11", "-target", "11"),
        assembly / mainClass := EntryPoint,
        run / mainClass := EntryPoint,
        reStart / mainClass := EntryPoint,
        Compile / scalacOptions ~= {
          _.filterNot(Set("-Werror", "-Xfatal-warnings"))
        },
        assemblyJarName := "smarthome.jar",
        addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full),
        addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
        testFrameworks += new TestFramework("munit.Framework"),
        assembly / assemblyMergeStrategy := {
          case PathList(ps @ _*) if ps.last endsWith "io.netty.versions.properties" => MergeStrategy.first
          case x =>
            val oldStrategy = (assembly / assemblyMergeStrategy).value
            oldStrategy(x)
        },

        // use the results of aspectj weaving
        Aspectj/aspectjInputs += (Aspectj/aspectjCompiledClasses).value,
        Compile/products := (Aspectj/products).value,
        Runtime/products := (Compile/products).value,
        Test/products := (Test/products).value,
        Global / onChangedBuildSource := ReloadOnSourceChanges
      )
```

License
-------

This code is open source software licensed under the [Apache 2.0 License][apache]. Feel free to use it accordingly.


[sbt]: https://github.com/sbt/sbt
[aspectj]: http://www.eclipse.org/aspectj
[releases]: https://github.com/sbt/sbt-aspectj/releases
[samples]: https://github.com/sbt/sbt-aspectj/tree/master/src/sbt-test
[cla]: https://www.lightbend.com/contribute/cla
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html

AspectJ sbt plugin
==================

Add plugin
----------

Add plugin to `project/plugins` build. For example:

    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

    libraryDependencies += "com.typesafe.sbt-aspectj" %% "sbt-aspectj" % "0.4.3"


Example settings
----------------

Set the aspectj inputs, the jars which should be instrumented. This is an ordered sequence of jars where instrumentation can be chained.

  inputs in Aspectj <<= update map { report =>
    report.matching(moduleFilter(organization = "se.scalablesolutions.akka", name = "akka-actor" | "akka-remote")).sortBy(_.name)
  }

Set the aspect filter, to map jars to aspects:

    aspectFilter in Aspectj := {
      (jar, aspects) => {
        if (jar.name.contains("akka-actor")) aspects filter (_.name.startsWith("Actor"))
        else if (jar.name.contains("akka-remote")) aspects filter (_.name.startsWith("Remote"))
        else Seq.empty[File]
      }
    }

Replace the original jars in the test classpath with the instrumented jars:

    fullClasspath in Test <<= AspectjPlugin.useInstrumentedJars(Test)


Weave
-----

The command to run the aspectj compiler is `aspectj:weave`.

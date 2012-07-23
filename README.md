AspectJ sbt plugin
==================

[sbt] plugin for weaving with [aspectj]. This plugin requires sbt 0.11.3.

[sbt]: https://github.com/harrah/xsbt
[aspectj]: http://www.eclipse.org/aspectj/


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

    resolvers += Classpaths.typesafeResolver

    addSbtPlugin("com.typesafe.sbtaspectj" % "sbt-aspectj" % "0.5.2")


Sample project
--------------

There is a runnable sample project included under [sample].

[sample]: https://github.com/typesafehub/sbt-aspectj/tree/master/sample


Example settings
----------------

Set the aspectj inputs, the jars which should be instrumented. This is an
ordered sequence of jars where instrumentation can be chained.

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


Contribution policy
-------------------

Contributions via GitHub pull requests are gladly accepted from their original
author. Along with any pull requests, please state that the contribution is your
original work and that you license the work to the project under the project's
open source license. Whether or not you state this explicitly, by submitting any
copyrighted material via pull request, email, or other means you agree to
license the material under the project's open source license and warrant that
you have the legal authority to do so.


License
-------

This code is open source software licensed under the [Apache 2.0 License]
[apache]. Feel free to use it accordingly.

[apache]: http://www.apache.org/licenses/LICENSE-2.0.html

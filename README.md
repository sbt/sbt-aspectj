sbt-aspectj
===========

[sbt] plugin for weaving with [aspectj]. This plugin requires sbt 0.12.x.


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

    addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.6.0")


Sample projects
---------------

There are runnable sample projects included under [sample].


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

    fullClasspath in Test <<= SbtAspectj.useInstrumentedJars(Test)


Weave
-----

The command to run the aspectj compiler is `aspectj:weave`.


Mailing list
------------

Please use the [sbt mailing list][email] and prefix the subject with `[sbt-aspectj]`.


Contribution policy
-------------------

Contributions via GitHub pull requests are gladly accepted from their original
author. Before we can accept pull requests, you will need to agree to the
[Typesafe Contributor License Agreement][cla] online, using your GitHub account.


License
-------

This code is open source software licensed under the [Apache 2.0 License]
[apache]. Feel free to use it accordingly.


[sbt]: https://github.com/harrah/xsbt
[aspectj]: http://www.eclipse.org/aspectj
[sample]: https://github.com/sbt/sbt-aspectj/tree/master/sample
[email]: http://groups.google.com/group/simple-build-tool
[cla]: http://www.typesafe.com/contribute/cla
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html

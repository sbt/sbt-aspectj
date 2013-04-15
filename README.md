sbt-aspectj
===========

[sbt] plugin for weaving with [aspectj]. This plugin requires sbt 0.12.


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

    addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.8.1")


Sample projects
---------------

There are [runnable sample projects][samples] included as sbt scripted tests.


Example settings
----------------

Set the aspectj inputs:

```scala
inputs in Aspectj <<= update map { report =>
  report.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))
}
```

Replace the original jars in the test classpath with the instrumented classes:

```scala
fullClasspath in Test <<= SbtAspectj.useInstrumentedClasses(Test)
```


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
[samples]: https://github.com/sbt/sbt-aspectj/tree/master/src/sbt-test
[email]: http://groups.google.com/group/simple-build-tool
[cla]: http://www.typesafe.com/contribute/cla
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html

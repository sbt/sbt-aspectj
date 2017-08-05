sbt-aspectj
===========

[sbt] plugin for weaving with [aspectj]. This plugin requires sbt 0.13.5+.

[![Build Status](https://travis-ci.org/sbt/sbt-aspectj.png?branch=master)](https://travis-ci.org/sbt/sbt-aspectj)


Add plugin
----------

Add plugin to `project/plugins.sbt`. For example:

    addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj" % "{version}")

See [released versions][releases].

Note: earlier versions of sbt-aspectj used the `"com.typesafe.sbt"` organization.


Sample projects
---------------

There are [runnable sample projects][samples] included as sbt scripted tests.


Contribution policy
-------------------

Contributions via GitHub pull requests are gladly accepted from their original
author. Before we can accept pull requests, you will need to agree to the
[Lightbend Contributor License Agreement][cla] online, using your GitHub account.


License
-------

This code is open source software licensed under the [Apache 2.0 License][apache]. Feel free to use it accordingly.


[sbt]: https://github.com/sbt/sbt
[aspectj]: http://www.eclipse.org/aspectj
[releases]: https://github.com/sbt/sbt-aspectj/releases
[samples]: https://github.com/sbt/sbt-aspectj/tree/master/src/sbt-test
[cla]: https://www.lightbend.com/contribute/cla
[apache]: http://www.apache.org/licenses/LICENSE-2.0.html

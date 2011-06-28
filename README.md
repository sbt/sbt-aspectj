AspectJ sbt plugin
==================

Add plugin
----------

Add plugin to `project/plugins` build. For example:

    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

    libraryDependencies += "com.typesafe" %% "aspectj-sbt-plugin" % "0.4.0"


Example settings
----------------

Set the input filter, which filters jars in the managed-classpath to those that
should be instrumented:

    inputFilter in Aspectj := {
      jar => { jar.name.startsWith("akka-actor") || jar.name.startsWith("akka-remote") }
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

    fullClasspath in Test <<= (fullClasspath in Test, aspectMappings in Aspectj, weave in Aspectj) map {
      (cp, mappings, woven) => {
        cp map { a => mappings.find(_.in == a.data).map(_.out).map(Attributed.blank).getOrElse(a) }
      }
    }


Weave
-----

The command to run the aspectj compiler is `aspectj:weave`.

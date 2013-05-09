# Weaving a jar file

A sample project that weaves the akka-actor jar.

To run the sample test, call `sbt check`.


## Inputs

The target jar files can be added as aspectj inputs by finding them in the
update report. For example:

```scala
AspectjKeys.inputs in Aspectj <++= update map { report =>
  report.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))
}
```


## Using instrumented classes

There is a helper method for replacing the original jar files in a classpath
with the instrumented classes. For example, to replace the original akka-actor
jar on the runtime classpath with the instrumented classes use:

```scala
fullClasspath in Runtime <<= useInstrumentedClasses(Runtime)
```

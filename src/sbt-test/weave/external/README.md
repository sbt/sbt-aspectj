# External compiled aspects

A sample project that uses already compiled and published aspects.

To run the sample test, call `sbt tracer/publish-local check`.


## Binary aspects

Binary aspects can be added with the `AspectjKeys.binaries` setting, which
corresponds to the `-aspectpath` option for the aspectj compiler.

The aspect jar files can be added as aspectj binaries by finding them in the
update report. For example:

```scala
binaries in Aspectj ++= update.value map.matching(moduleFilter(organization = "org.something", name = "some-aspects"))
```

# Load-time weaving

A sample project that compiles aspects and weaves at load-time.

To run the sample test, call `sbt check`.


# Compile aspects

To only compile aspects (the `-XterminateAfterCompilation` aspectj option) use:

```scala
AspectjKeys.compileOnly in Aspectj := true
```

The compiled aspects can be added to the regular compile products, for packaging
or using from other subprojects, with:

```scala
products in Compile ++= (products in Aspectj).value
```


# Load-time weaving

To weave at load-time the compiled aspects should be on the classpath and the
aspectj weaver added as a java agent.

On the command-line, the java agent is added with something like:

```bash
java -javaagent:/path/to/aspectjweaver.jar ...
```

In sbt, set run to be forked so that the java agent option can be added:

```scala
fork in run := true
```

Add the aspectj weaver options provided by sbt-aspectj:

```
javaOptions in run ++= (AspectjKeys.weaverOptions in Aspectj).value
```

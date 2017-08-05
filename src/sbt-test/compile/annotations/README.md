# Precompiled annotation-based aspects

A sample project that compiles annotation-based aspects written in Scala.

This sample is useful for creating binary aspects for later compile-time or
load-time weaving.

To run the sample test, call `sbt check`.

## Adding inputs

To weave annotation-based aspects, add the compiled classes as an input. There
is a helper method called `aspectjCompiledClasses` for doing this:

```scala
aspectjInputs in Aspectj += (aspectjCompiledClasses in Aspectj).value
```


## Ignoring warnings

When the aspects are being precompiled without the target types aspectj will
generate lint warnings. These can be ignored by adding lint properties.
For example:

```scala
aspectjLintProperties in Aspectj += "invalidAbsoluteTypeName = ignore"
```

```scala
aspectjLintProperties in Aspectj += "adviceDidNotMatch = ignore"
```


## Products

To package the compiled aspects, replace the regular compile products with the
aspectj products:

```scala
products in Compile := (products in Aspectj).value
```

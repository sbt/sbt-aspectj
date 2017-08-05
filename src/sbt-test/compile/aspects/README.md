# Precompiled aspects

A sample project that compiles aspects.

This sample is useful for creating binary aspects for later compile-time or
load-time weaving.

To run the sample test, call `sbt check`.


## Compile-only

To only compile aspects and not weave, set the `aspectjCompileOnly` option.

```scala
aspectjCompileOnly in Aspectj := true
```


## Ignoring warnings

When the aspects are being precompiled without the target types aspectj will
generate lint warnings. These can be ignored by adding lint properties.
For example:

```scala
aspectjLintProperties in Aspectj += "invalidAbsoluteTypeName = ignore"
```


## Products

To package the compiled aspects, replace the regular compile products with the
aspectj products:

```scala
products in Compile := (products in Aspectj).value
```

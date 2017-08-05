# Weaving annotation-based aspects

A sample project that weaves annotation-based aspects written in Scala.

To run the sample test, call `sbt check`.


## Adding inputs

To weave annotation-based aspects, add the compiled classes as an input. There
is a helper method called `aspectjCompiledClasses` for doing this:

```scala
aspectjInputs in Aspectj += (aspectjCompiledClasses in Aspectj).value
```


## Products

To use or package the compiled aspects, replace the regular compile products
with the aspectj products:

```scala
products in Compile := (products in Aspectj).value
```

And to have these same products also in runtime:

```scala
products in Runtime := (products in Compile).value
```

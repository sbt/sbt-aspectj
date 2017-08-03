package com.typesafe.sbt

import sbt.File

case class AspectjOptions(
  showWeaveInfo: Boolean,
  verbose: Boolean,
  compileOnly: Boolean,
  outXml: Boolean,
  sourceLevel: String,
  lintProperties: Option[File],
  extraOptions: Seq[String]
)

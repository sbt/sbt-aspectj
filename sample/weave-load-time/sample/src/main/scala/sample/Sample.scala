/**
 *  Copyright (C) 2011 Typesafe, Inc <http://typesafe.com>
 */

package sample

class Sample {
  def printSample() = println("sample")
}

object Sample extends App {
  val sample = new Sample
  sample.printSample()
}

package sample

class Sample {
  def printSample() = println("hello")
}

object Sample extends App {
  val sample = new Sample
  sample.printSample()
}

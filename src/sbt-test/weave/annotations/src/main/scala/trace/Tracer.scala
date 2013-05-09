package trace

import org.aspectj.lang.annotation._

@Aspect
class Tracer {

  @Before("execution(* sample.Sample.printSample(..))")
  def printSample() {
    println("Printing sample:")
  }
}

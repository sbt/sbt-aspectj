package sample;

import org.aspectj.lang.annotation.*;

@Aspect
public class SampleAspect {

  @Before("execution(* sample.Sample.printSample(..))")
  public void printSample() {
    System.out.println("Printing sample:");
  }
}

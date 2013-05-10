package sample;

privileged public aspect SampleAspect {

  before():
    execution(* sample.Sample.printSample(..))
  {
    System.out.println("Printing sample:");
  }
}

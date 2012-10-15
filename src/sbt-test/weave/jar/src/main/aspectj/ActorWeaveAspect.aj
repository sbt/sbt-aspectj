package sample;

import akka.actor.ScalaActorRef;

privileged public aspect ActorWeaveAspect {

  // print on ask

  before(ScalaActorRef scalaActorRef, Object message):
    execution(* akka.actor.ScalaActorRef$class.$qmark(..)) &&
    args(scalaActorRef, message, ..)
  {
    System.out.println("Actor asked " + message);
  }
}

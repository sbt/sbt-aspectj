package sample;

import akka.actor.ActorRef;
import akka.pattern.AskSupport;

privileged public aspect ActorWeaveAspect {

  // print on ask

  before(AskSupport askSupport, ActorRef actorRef, Object message):
    execution(* akka.pattern.AskSupport$class.ask(..)) &&
    args(askSupport, actorRef, message, ..)
  {
    String msg = message.toString();
    if (!msg.startsWith("InitializeLogger")) {
      System.out.println("Actor asked " + message);
    }
  }
}

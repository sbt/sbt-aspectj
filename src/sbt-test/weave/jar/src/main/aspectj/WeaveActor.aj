package sample;

import akka.actor.ActorRef;

privileged public aspect WeaveActor {

  // print on ask

  before(ActorRef actorRef, Object message):
    execution(* akka.pattern.AskableActorRef$.internalAsk*(..)) &&
    args(actorRef, message, ..)
  {
    String msg = message.toString();
    if (!msg.startsWith("InitializeLogger")) {
      System.out.println("Actor asked " + message);
    }
  }
}

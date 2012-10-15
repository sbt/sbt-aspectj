package sample

import akka.actor._

class SampleActor extends Actor {
  def receive = {
    case message: String => self.reply("hello " + message)
  }
}

object Sample extends App {
  val actor = Actor.actorOf[SampleActor].start

  println((actor ? "world").as[String].get)

  actor.stop
}

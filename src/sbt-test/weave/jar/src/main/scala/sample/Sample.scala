package sample

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

class SampleActor extends Actor {
  def receive = {
    case message: String => sender ! ("hello " + message)
  }
}

object Sample extends App {
  val system = ActorSystem("sample")
  val actor = system.actorOf(Props[SampleActor])

  implicit val timeout = Timeout(3.seconds)
  val result = Await.result(actor ? "world", timeout.duration)
  println(result)

  system.terminate()
}

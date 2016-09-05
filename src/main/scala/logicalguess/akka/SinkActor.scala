package logicalguess.akka

import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnNext}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import com.typesafe.scalalogging.LazyLogging


//Actor Subscriber trait extension is need so that this actor can be used as part of a stream
class SinkActor(name: String, delay: Long) extends ActorSubscriber with LazyLogging {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  val actorName = name

  def this(name: String) {
    this(name, 0)
  }

  override def receive: Receive = {
    case OnNext(msg: (String, Int)) =>
      println(msg)
      logger.debug(s"Message in actor sink ${self.path} '$actorName': $msg")
    case OnComplete =>
      logger.debug(s"Completed Messgae received in ${self.path} '$actorName'")
    case msg =>
      logger.debug(s"Unknown message $msg in $actorName: ")
  }
}
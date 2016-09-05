package logicalguess.spark

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import logicalguess.domain.{Event, RandomEvent}
import org.apache.spark.{SparkConf, SparkEnv}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.receiver.ActorHelper

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import Functions._
import org.apache.log4j.{Level, Logger}

object ActorStream {

  def main(args: Array[String]): Unit = {

    val receiverActorName = "receiver"

    val createFlow: (String, StreamingContext) => Unit = { (receiverActorName, ssc) =>
      val actorStream = ssc.actorStream[Event](Props[Receiver], receiverActorName)
      eventProcessor(actorStream)
    }

    val actorStream = Spark.init(createFlow.curried(receiverActorName))

    val actorSystem = SparkEnv.get.actorSystem

    val receiverActor = lookup(actorSystem, receiverActorName)

    val cancellable =
      actorSystem.scheduler.schedule(0.milliseconds, 10.milliseconds) {
        receiverActor ! RandomEvent()
      }

    actorSystem.scheduler.scheduleOnce(10 seconds) {
      cancellable.cancel()
    }

  }

  def lookup(actorSystem: ActorSystem, actorName: String): ActorRef = {
    val url = s"akka://sparkDriverActorSystem/user/Supervisor0/$actorName"
    val timeout = 10.seconds
    val receiver = Await.result(actorSystem.actorSelection(url).resolveOne(timeout), timeout)
    receiver
  }

  /** This actor is a bridge to Spark. It is in charge of receiving data */
  class Receiver extends Actor with ActorHelper {
    override def preStart() = {
      println(s"Starting Receiver actor at ${context.self.path}")
    }
    def receive = {
      case e: Event =>
        store(e)
    }
  }

  object Spark {
    def init(f: StreamingContext => Unit) = {
      val conf = new SparkConf(false)
        .setMaster("local[*]")
        .setAppName("Windowing with Spark Streaming")

      val ssc = new StreamingContext(conf, Seconds(1))

      val rootLogger = Logger.getRootLogger()
      rootLogger.setLevel(Level.ERROR)
      
      ssc.checkpoint("checkpoint")

      f(ssc)

      ssc.start()
      ssc.awaitTerminationOrTimeout(10000)
    }
  }

}
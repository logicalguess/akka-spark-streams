package logicalguess.spark

import akka.actor.ActorSystem
import logicalguess.domain.{Event, RandomEvent}
import logicalguess.spark.Functions._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.api.java.StorageLevels
import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ReceiverStream {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()

    val createFlow: (ActorSystem, StreamingContext) => Unit = { (actorSystem, ssc) =>
      eventProcessor(ssc.receiverStream(Source))

      actorSystem.scheduler.scheduleOnce(20 seconds) {
        //ssc.stop()
      }
    }

    Spark.init(createFlow.curried(actorSystem))

    val cancellable =
      actorSystem.scheduler.schedule(0 milliseconds, 100 milliseconds) {
        Source.store(RandomEvent())
      }

    actorSystem.scheduler.scheduleOnce(10 seconds) {
      cancellable.cancel()
    }
  }

  object Source extends Receiver[Event](StorageLevels.MEMORY_AND_DISK) {

    override def onStart() = {
    }

    override def onStop(): Unit = {
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
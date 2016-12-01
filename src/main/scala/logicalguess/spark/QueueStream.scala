package logicalguess.spark

import logicalguess.domain.{Event, RandomEvent}

import scala.collection.mutable.Queue
import org.apache.spark.{SparkConf, SparkEnv}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import Functions._
import akka.actor.ActorSystem
import org.apache.log4j.{Level, Logger}

object QueueStream {

  def main(args: Array[String]) {

    val sparkConf = new SparkConf().setAppName("QueueStream").setMaster("local")
    // Create the context
    val ssc = new StreamingContext(sparkConf, Seconds(1))

    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    ssc.checkpoint("checkpoint")

    // Create the queue through which RDDs can be pushed to
    // a QueueInputDStream
    val rddQueue = new Queue[RDD[Event]]()

    // Create the QueueInputDStream and use it do some processing
    val inputStream = ssc.queueStream(rddQueue)
    eventProcessor(inputStream)
    ssc.start()

    //publish events
    val actorSystem = ActorSystem()
    val cancellable =
      actorSystem.scheduler.schedule(0 milliseconds, 1 second) {
        rddQueue += ssc.sparkContext.makeRDD((1 to 100).map(_ => RandomEvent()), 10)
      }
    actorSystem.scheduler.scheduleOnce(10 seconds) {
      cancellable.cancel()
    }
    actorSystem.scheduler.scheduleOnce(20 seconds) {
      ssc.stop()
    }

    Console.in.read()
  }
}

package logicalguess.algebird.spark

import java.util.Random

import akka.actor.ActorSystem
import com.twitter.algebird._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.api.java.StorageLevels
import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Example of using HyperLogLog monoid from Twitter's Algebird together with Spark Streaming
 */
object StreamingHLL {
  def main(args: Array[String]) {


    val conf = new SparkConf(false)
      .setMaster("local[*]")
      .setAppName("StreamingHLL")

    val ssc = new StreamingContext(conf, Seconds(1))
    val stream = ssc.receiverStream(Source)

    val users = stream.map(id => id)

    val globalHll = new HyperLogLogMonoid(12)
    var userSet: Set[String] = Set()

    val approxUsers = users.mapPartitions(ids => {
      val hll = new HyperLogLogMonoid(12)
      ids.map(id => hll(id.getBytes("utf-8")))
    }).reduce(_ + _)

    val exactUsers = users.map(id => Set(id)).reduce(_ ++ _)

    var h = globalHll.zero
    approxUsers.foreachRDD(rdd => {
      if (rdd.count() != 0) {
        val partial = rdd.first()
        h += partial
        println("Approx distinct users this batch: %d".format(partial.estimatedSize.toInt))
        println("Approx distinct users overall: %d".format(globalHll.estimateSize(h).toInt))
      }
    })

    exactUsers.foreachRDD(rdd => {
      if (rdd.count() != 0) {
        val partial = rdd.first()
        userSet ++= partial
        println("Exact distinct users this batch: %d".format(partial.size))
        println("Exact distinct users overall: %d".format(userSet.size))
        println("Error rate: %2.5f%%".format(((globalHll.estimateSize(h) / userSet.size.toDouble) - 1) * 100))
      }
    })

    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    ssc.checkpoint("checkpoint")
    ssc.start()

    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._

    val actorSystem = ActorSystem()

    val cancellable =
      actorSystem.scheduler.schedule(0 milliseconds, 100 milliseconds) {
        Source.store(java.util.UUID.randomUUID.toString)
      }

    actorSystem.scheduler.scheduleOnce(20 seconds) {
      cancellable.cancel()
      ssc.stop()
      println("Final error rate: %2.5f%%".format(((globalHll.estimateSize(h) / userSet.size.toDouble) - 1) * 100))
      System.exit(0)
    }
  }

  object Source extends Receiver[String](StorageLevels.MEMORY_AND_DISK) {

    override def onStart() = {
    }

    override def onStop(): Unit = {
    }
  }
}
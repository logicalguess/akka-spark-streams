package logicalguess.spark

import logicalguess.domain.Event
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream


object Functions {
  val eventProcessor: DStream[Event] => Unit = { stream =>
    val categoryCount = stream.map(event => (event.category, 1))
    val grouped = categoryCount.reduceByKeyAndWindow(_ + _, _ - _, Seconds(1), Seconds(1))
    grouped.foreachRDD(rdd => rdd.foreach {
      case (category, count) => println("count per window - " + category + ":" + count)}
    )
  }

}

package logicalguess.akka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import logicalguess.domain.{Event, RandomEvent}

import scala.concurrent.duration._

object FluentStream {
  implicit val sys = ActorSystem("streams", ConfigFactory.empty())
  implicit val ec = sys.dispatcher
  implicit val materializer = ActorMaterializer()


  case object Tick
  val infinite: Source[Event, _] = Source.tick(0 millis, 100 millis, Tick).map(_ => RandomEvent())

  val finite: Source[Event, _] = Source.fromIterator(() => (1 to 100).iterator).map(_ => RandomEvent())

  def infiniteSource = {
    println("\nInfinite Source\n----------------")


    //    val processor = Flow[Event]
    //      .filter(_.category == "error") //groupBy?
    //      .groupedWithin(Int.MaxValue, 1.second)
    //      .map[Int](_.length)

    val processor = Flow[Event]
      .groupedWithin(Int.MaxValue, 1.second)
      .map(w => w.groupBy(_.category).map { case (category, events) => category -> events.length })

    infinite.via(processor).runWith(Sink.foreach(println))
  }

  def finiteSource = {
    println("\nFinite Source\n----------------")

    val processor = Flow[Event]
      .groupBy(3, _.category)
      //.groupedWithin(Int.MaxValue, 10.millis)
      .fold(("", 0)) {
        //(acc: (String, Int), es: Seq[Event]) =>(es(0).category, acc._2 + 1)
      (acc: (String, Int), e: Event) =>(e.category.toString, acc._2 + 1)
    }.mergeSubstreams

    finite.via(processor).runWith(Sink.foreach(println))
  }

  def main(args: Array[String]): Unit = {
    for {
      _ <- finiteSource
      _ <- infiniteSource
    } yield ()
  }

}

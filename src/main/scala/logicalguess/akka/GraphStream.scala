package logicalguess.akka

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import com.typesafe.config.ConfigFactory
import logicalguess.domain.{Category, Event, RandomEvent}

import scala.concurrent.duration._

object GraphStream {
  implicit val sys = ActorSystem("streams", ConfigFactory.empty())
  implicit val ec = sys.dispatcher
  implicit val materializer = ActorMaterializer()

  type Transformer[In, Out] = Graph[FlowShape[In, Out], NotUsed/*Any*/]

  case object Tick
  val events: Source[Event, _] = Source.tick(0 millis, 100 millis, Tick).map(_ => RandomEvent())

  def countsByCategoryMap = {
    println("\nMap\n----------------")

    val processor: Transformer[Event, Map[String, Int]] = Flow[Event]
      .groupedWithin(Int.MaxValue, 1.second)
      .map(w => w.groupBy(_.category).map { case (category, events) => category.toString -> events.length })

    val g = RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        // Source
        val A: Outlet[Event] = builder.add(events).out

        // Flows
        val B: FlowShape[Event, Map[String, Int]] = builder.add(processor)

        // Sinks
        val C: Inlet[Any] = builder.add(Sink.foreach(println)).in

        A ~> B ~> C

        ClosedShape
    })

    g.run()
  }

  def countsByCategoryFilter = {
    println("\nFilter\n----------------")


    def processor(category: String): Transformer[Event, (String, Int)] = Flow[Event]
      .filter(_.category.toString == category)
      .groupedWithin(Int.MaxValue, 1.second)
      .map[(String, Int)](es => (category, es.length))

    val g = RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        // Source
        val A: Outlet[Event] = builder.add(events).out

        // Flows
        val B: UniformFanOutShape[Event, Event] = builder.add(Broadcast[Event](3))

        val C: FlowShape[Event, (String, Int)] = builder.add(processor(Category.info.toString))
        val D: FlowShape[Event, (String, Int)] = builder.add(processor(Category.warning.toString))
        val E: FlowShape[Event, (String, Int)] = builder.add(processor(Category.error.toString))

        val F: UniformFanInShape[(String, Int), (String, Int)] = builder.add(Merge[(String, Int)](3))

        // Sinks
        //val G: Inlet[Any] = builder.add(Sink.foreach(println)).in
        val G: SinkShape[Any] = builder.add(Sink.actorSubscriber(Props(classOf[SinkActor], "sinkActor")))

        A ~> B ~> C ~> F ~> G
             B ~> D ~> F
             B ~> E ~> F

        ClosedShape
    })

    g.run()
  }

  def main(args: Array[String]): Unit = {
    //countsByCategoryMap
    countsByCategoryFilter
  }

}

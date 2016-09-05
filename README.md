#Domain

    object Category extends Enumeration {
      val info, warning, error = Value
    }

    /**Event model */
    case class Event(guid: String, category: Category.Value, action: String, timestamp: String)

#Spark Streaming

    object Functions {
      val eventProcessor: DStream[Event] => Unit = { stream =>
        val categoryCount = stream.map(event => (event.category, 1))
        val grouped = categoryCount.reduceByKeyAndWindow(_ + _, _ - _, Seconds(1), Seconds(1))
        grouped.foreachRDD(rdd => rdd.foreach {
          case (category, count) => println("count per window - " + category + ":" + count)}
        )
      }
    }

##
        val rddQueue = new Queue[RDD[Event]]()

        // Create the QueueInputDStream and use it do some processing
        val inputStream = ssc.queueStream(rddQueue)
        eventProcessor(inputStream)
        ssc.start()

        //publish events
        val actorSystem = SparkEnv.get.actorSystem
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

#Akka Streaming

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

##

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

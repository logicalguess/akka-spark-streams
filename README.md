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
    
            val F = builder.add(Merge[(String, Int)](3))
    
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
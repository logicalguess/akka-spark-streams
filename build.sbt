name := "akka-spark-streams"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion  = "2.4.9"
val kamonVersion = "0.5.2"
val sparkVersion = "2.0.0"

/* dependencies */
libraryDependencies ++= Seq (

  // -- Testing --
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"

  // -- Logging --
  ,"ch.qos.logback" % "logback-classic" % "1.1.2"
  ,"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

  // -- Akka --
  ,"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  ,"com.typesafe.akka" %% "akka-actor" % akkaVersion
  ,"com.typesafe.akka" %% "akka-slf4j" % akkaVersion

  // --Akka Streams
  ,"com.typesafe.akka" %% "akka-stream" % akkaVersion

  ,"org.apache.spark"  %% "spark-streaming" % sparkVersion
  ,"org.apache.spark"  %% "spark-sql" % sparkVersion

  ,"com.twitter" %% "algebird-core" % "0.13.0"


// -- Config --
  ,"com.typesafe" % "config" % "1.2.1"

)

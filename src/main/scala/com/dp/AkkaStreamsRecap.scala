package com.dp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.util.{Failure, Success}

object AkkaStreamsRecap extends App {

  implicit val system = ActorSystem("AkkaStreamsRecap")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val source = Source(1 to 100)
  val sink = Sink.foreach[Int](println)
  val flow = Flow[Int].map(_ + 1)

  val runnableGraph = source.via(flow).to(sink)
  val simpleMaterializedValue = runnableGraph.run() // materialization

  //Run method takes the materialzer defined allocates the
  // resources to start the akka stream
  // MATERIALIZED VALUE
  runnableGraph.run()

  //Sink
  val sumSink = Sink.fold[Int, Int](0)((currentSum ,element) => currentSum + element)
  // sumFuture is the materialized value of sumSink
  val sumFuture = source.runWith(sumSink)

  sumFuture.onComplete {
    case Success(sum) => println(s"The sum of all the numbers from the simple source is: $sum")
    case Failure(ex) => println(s"Summing of all the numbers failed: $ex")
  }
}

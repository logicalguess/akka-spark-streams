package logicalguess.algebird.simple

import scala.io.Source

object HLLWordCount {
  import com.twitter.algebird.HyperLogLogMonoid

  def main(args: Array[String]) {
    // define test data
    //val data = Seq("aaa", "bbb", "ccc")

    val alice: Stream[String] = Source.fromFile("src/main/resources/alice.txt").getLines.toStream
    val aliceWords: Stream[String] = alice.flatMap(_.toLowerCase.split("\\s+"))

    // create algebird HLL
    val hll = new HyperLogLogMonoid(bits = 10)
    // convert data elements to a seq of hlls
    val hlls = aliceWords.map { str =>
      val bytes = str.getBytes("utf-8")
      hll.create(bytes)
    }

    // merge seq of hlls in one hll object
    val merged = hll.sum(hlls)

    // WARN: don`t use merged.size - it is a different thing
    // get the estimate count from merged hll
    // scalastyle:off println
    println("estimate count: " + hll.sizeOf(merged).estimate)
    // or
    println("estimate count: " + merged.approximateSize.estimate)
    // scalastyle:on println

  }
}
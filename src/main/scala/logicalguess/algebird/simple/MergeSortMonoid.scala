package logicalguess.algebird.simple

import com.twitter.algebird.Monoid

import scala.annotation.tailrec
import scala.collection.GenSeq
import scala.collection.immutable.Queue

class MergeSortMonoid[T](implicit ord: Ordering[T]) extends Monoid[Queue[T]] {
  def build(value: T) = Queue(value)

  override def zero: Queue[T] = Queue()

  override def plus(l: Queue[T], r: Queue[T]): Queue[T] = {
    @tailrec
    def plusRec(l: Queue[T], r: Queue[T], acc: Queue[T]): Queue[T] = {
      if(l.isEmpty)
        acc.enqueue(r)
      else if(r.isEmpty)
        acc.enqueue(l)
      else if(ord.lteq(l.head, r.head)) {
        val(head, tail) = l.dequeue
        plusRec(tail, r, acc.enqueue(head))
      } else {
        val(head, tail) = r.dequeue
        plusRec(l, tail, acc.enqueue(head))
      }
    }

    plusRec(l, r, Queue())
  }
}

object MergeSortMonoid {
  implicit def mergeSortMonoid[T](implicit ord: Ordering[T]) = new MergeSortMonoid[T]

  implicit class PimpSeq[T](val seq: GenSeq[T]) extends AnyVal {
    def mergeSort(implicit monoid: MergeSortMonoid[T]): Seq[T] =
      seq.aggregate(monoid.zero)({ case (q, value) => monoid.plus(q, monoid.build(value)) }, monoid.plus)
  }
}

object Test {
  import MergeSortMonoid._
  def main(args: Array[String]): Unit = {
    println(List(1, 7, 3, 15, 2).mergeSort)
  }
}
package algebird.bayes

import logicalguess.algebird.bayes.{Bayes, BayesMonoid, BayesPmf, BayesPos}
import org.scalatest.PropSpec
import thinkbayes.Pmf

/**
  * Created by logicalguess on 6/18/17.
  */
class BayesMonoidTest extends PropSpec {

  implicit def BayesToBayesPmf(Bayes: Bayes[_]) = Bayes.asInstanceOf[BayesPmf]
  implicit def BayesPosToBayesPmf(Bayes: BayesPos[_]) = Bayes.asInstanceOf[BayesPmf]
  implicit def BayesToBayesWPos[Pos](Bayes: Bayes[Pos]) = Bayes.asInstanceOf[BayesPos[Pos]]

  property("rolling dice with BayesMonoid") {

    val likelihood: (Int, Int) => Double = (h: Int, d: Int) => if (h < d) 0 else 1.0 / h
    val bayesMonoid = BayesMonoid(likelihood)

    println("Priors:")
    val pmf = Pmf(List(4, 6, 8, 12, 20))
    pmf.printChart()

    println()
    println("After a 6 is rolled:")
    val bpfm = bayesMonoid.plus(BayesPmf(pmf), BayesPos(List(6)))
    bpfm.pmf.printChart()

    println()
    println("After 6, 8, 7, 7, 5, 4 are rolled after the first 6:")
    val bpfm1 = bayesMonoid.plus(bpfm, BayesPos(List(6, 8, 7, 7, 5, 4)))
    bpfm1.pmf.printChart()
    
  }

}

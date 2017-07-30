package logicalguess.algebird.bayes

import com.twitter.algebird.Monoid
import thinkbayes.Pmf


sealed abstract class Bayes[+Data, +Hypothesis]
case object BayesZero extends Bayes[Nothing, Nothing]
case class BayesData[Data](val ds: List[Data]) extends Bayes[Data, Nothing]
case class BayesPmf[Hypothesis](val pmf: Pmf[Hypothesis]) extends Bayes[Nothing, Hypothesis]

case class BayesMonoid[Data, Hypothesis](likelihood: (Data, Hypothesis) => Double)
  extends Monoid[Bayes[Data, Hypothesis]] {

  val zero = BayesZero

  def plus(left: Bayes[Data, Hypothesis], right: Bayes[Data, Hypothesis]): Bayes[Data, Hypothesis] = {
    (left, right) match {
      case (_, BayesZero) => left
      case (BayesData(lds), BayesData(rds)) => BayesData(lds ::: rds)
      case (BayesPmf(pmf), BayesData(ds)) => BayesPmf(ds.foldLeft(pmf.asInstanceOf[Pmf[Hypothesis]]) { (current, d) =>
        newPmf(current.toPmf, d)
      })
      // TODO make a RightFolded2 which folds A,B => (B,C), and a group on C.
      case _ => right
    }
  }

  def newPmf(pmf: Pmf[Hypothesis], d: Data): Pmf[Hypothesis] = {
    //val pmf = bpmf.pmf
    val newPmf: Pmf[Hypothesis] = pmf.map { case (h, prob) => (h, prob * likelihood(d, h)) }.normalized
    //BayesPmf(newPmf)
    newPmf
  }
}

object Bayes {
  implicit def BayesToBayesPmf[Hypothesis, Data](b: Bayes[Data, Hypothesis]) = b.asInstanceOf[BayesPmf[Hypothesis]]

  implicit def BayesDataToBayesPmf[Hypothesis, Data](b: BayesData[Data]) = b.asInstanceOf[BayesPmf[Hypothesis]]

  implicit def BayesToBayesData[Data](b: Bayes[Data, _]) = b.asInstanceOf[BayesData[Data]]

  implicit def PmfToBayesPmf[Hypothesis](pmf: Pmf[Hypothesis]) = BayesPmf(pmf)
  implicit def DataToBayesData[Data](p: Data) = BayesData[Data](List(p))

}

package logicalguess.algebird.bayes

import com.twitter.algebird.Monoid
import thinkbayes.Pmf


sealed abstract class Bayes[+Pos]
case object BayesZero extends Bayes[Nothing]
case class BayesPos[+Pos](val pos: List[Pos]) extends Bayes[Pos]
case class BayesPmf(val pmf: Pmf[Int]) extends Bayes[Nothing]

case class BayesMonoid[Pos](likelihood: (Int, Pos) => Double)
  extends Monoid[Bayes[Pos]] {

  val zero = BayesZero

  def plus(left: Bayes[Pos], right: Bayes[Pos]): Bayes[Pos] = {
    (left, right) match {
      case (_, BayesZero) => left
      case (BayesPos(llps), BayesPos(rlps)) => BayesPos(llps ::: rlps)
      case (pmf @ BayesPmf(_), BayesPos(p)) => p.foldLeft(pmf) { (current, pos) =>
        newPmf(current, pos)
      }
      // TODO make a RightFolded2 which folds A,B => (B,C), and a group on C.
      case _ => right
    }
  }

  def newPmf(bpmf: BayesPmf, p: Pos): BayesPmf = {
    val pmf = bpmf.pmf
    val newPmf = pmf.map { case (h, prob) => (h, prob * likelihood(h, p)) }.normalized
    BayesPmf(newPmf)
  }
}

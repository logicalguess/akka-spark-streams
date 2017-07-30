package logicalguess.algebird.bayes

import com.twitter.algebird.Monoid
import thinkbayes.Pmf


sealed abstract class Bayes[+Pos]
case object BayesZero extends Bayes[Nothing]
case class BayesPos[+Pos](val pos: List[Pos]) extends Bayes[Pos]
case class BayesPmf[Pos](val pmf: Pmf[Pos]) extends Bayes[Nothing]

case class BayesMonoid[Pos](likelihood: (Int, Pos) => Double)
  extends Monoid[Bayes[Pos]] {

  val zero = BayesZero

  def plus(left: Bayes[Pos], right: Bayes[Pos]): Bayes[Pos] = {
    (left, right) match {
      case (_, BayesZero) => left
      case (BayesPos(llps), BayesPos(rlps)) => BayesPos(llps ::: rlps)
      case (BayesPmf(pmf), BayesPos(p)) => BayesPmf(p.foldLeft(pmf.asInstanceOf[Pmf[Int]]) { (current, pos) =>
        newPmf(current.toPmf.asInstanceOf[Pmf[Int]], pos)
      })
      // TODO make a RightFolded2 which folds A,B => (B,C), and a group on C.
      case _ => right
    }
  }

  def newPmf(pmf: Pmf[Int], p: Pos): Pmf[Int] = {
    //val pmf = bpmf.pmf
    val newPmf: Pmf[Int] = pmf.map { case (h, prob) => (h, prob * likelihood(h, p)) }.normalized
    //BayesPmf(newPmf)
    newPmf
  }
}

object Bayes {
  implicit def BayesToBayesPmf[Pos](b: Bayes[Pos]) = b.asInstanceOf[BayesPmf[Pos]]

  implicit def BayesPosToBayesPmf[Pos](b: BayesPos[Pos]) = b.asInstanceOf[BayesPmf[Pos]]

  implicit def BayesToBayesPos[Pos](b: Bayes[Pos]) = b.asInstanceOf[BayesPos[Pos]]

  implicit def PmfToBayesPmf(pmf: Pmf[Int]) = BayesPmf(pmf)
  implicit def IntToBayesPos[Pos](p: Pos) = BayesPos[Pos](List(p))
}

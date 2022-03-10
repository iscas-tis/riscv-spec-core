package rvspeccore.core.spec

import chisel3._
import chisel3.util._

import instset._

case class Inst(bitPat: Option[BitPat], checker: Option[(UInt, Int) => Bool]) {
  def check(inst: UInt)(implicit XLEN: Int): Bool = {
    (bitPat, checker) match {
      case (Some(x), Some(y)) => inst === x && y(inst, XLEN)
      case (Some(x), None)    => inst === x
      case (None, Some(y))    => y(inst, XLEN)
      case (None, None)       => throw new Exception("bitPat or checker not defined")
    }
  }
  def apply(inst: UInt)(implicit XLEN: Int): Bool = check(inst)(XLEN)
}

object Inst {
  def apply(bits: String)                               = new Inst(Some(BitPat(bits)), None)
  def apply(checker: (UInt, Int) => Bool)               = new Inst(None, Some(checker))
  def apply(bits: String, checker: (UInt, Int) => Bool) = new Inst(Some(BitPat(bits)), Some(checker))
}

trait RVG extends RVI with RVM

trait RVInsts extends RVG

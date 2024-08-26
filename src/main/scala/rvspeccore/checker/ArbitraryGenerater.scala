package rvspeccore.checker

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

object ArbitraryRegFile {
  val uniqueIdArbitraryRegFile = "ArbitraryRegFile"
  def gen(implicit XLEN: Int): Vec[UInt] = {
    val initval = Wire(Vec(32, UInt(XLEN.W)))
    initval := DontCare
    BoringUtils.addSink(initval, uniqueIdArbitraryRegFile)
    initval
  }
  def init(implicit XLEN: Int): Vec[UInt] = {
    val rf = Wire(Vec(32, UInt(XLEN.W)))
    // rf.map(_ := DontCare)
    rf.map(_ := 0.U)
    rf(1) := DontCare
    rf(2) := DontCare
    BoringUtils.addSource(rf, uniqueIdArbitraryRegFile)
    rf
  }
}

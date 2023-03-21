package rvspeccore.checker

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rvspeccore.core.RVConfig
import rvspeccore.core.spec.instset.csr.CSR

abstract class ConnectHelper {}

/** Connect RegFile to io.result.reg by BoringUtils
  */
object ConnectCheckerResult extends ConnectHelper {
  val uniqueIdReg: String = "ConnectCheckerResult-UniqueIdReg"
  val uniqueIdMem: String = "ConnectCheckerResult-UniqueIdMem"
  val uniqueIdCSR: String = "ConnectCheckerResult-uniqueIdCSR"

  def setRegSource(regVec: Vec[UInt]) = {
    BoringUtils.addSource(regVec, uniqueIdReg)
  }
  class MemOneSig()(implicit XLEN: Int) extends Bundle {
    val valid    = Bool()
    val addr     = UInt(XLEN.W)
    val memWidth = UInt(log2Ceil(XLEN + 1).W)
    val data     = UInt(XLEN.W)
  }
  class MemSig()(implicit XLEN: Int) extends Bundle {
    val read  = new MemOneSig
    val write = new MemOneSig
  }
  def makeMemSource()(implicit XLEN: Int) = {
    val mem = Wire(new MemSig)

    mem.read.valid     := false.B
    mem.read.addr      := 0.U
    mem.read.data      := 0.U
    mem.read.memWidth  := 0.U
    mem.write.valid    := false.B
    mem.write.addr     := 0.U
    mem.write.data     := 0.U
    mem.write.memWidth := 0.U

    BoringUtils.addSource(mem, uniqueIdMem)

    mem
  }
  def regNextDelay[T <: Bundle](signal: T, delay: Int): T = {
    delay match {
      case 0 => signal
      case _ => regNextDelay(RegNext(signal), delay - 1)
    }
  }

  def makeCSRSource()(implicit XLEN: Int, config: RVConfig): CSR = {
    val csr = Wire(CSR())
    csr := DontCare
    BoringUtils.addSource(csr, uniqueIdCSR)
    csr
  }
  def setChecker(checker: CheckerWithResult, memDelay: Int = 0)(implicit XLEN: Int, config: RVConfig) = {
    // reg
    val regVec = Wire(Vec(32, UInt(XLEN.W)))
    regVec := DontCare
    BoringUtils.addSink(regVec, uniqueIdReg)

    checker.io.result.reg := regVec
    checker.io.result.pc  := DontCare

    if (checker.io.mem != None) {
      val mem = Wire(new MemSig)
      mem := DontCare
      BoringUtils.addSink(mem, uniqueIdMem)

      checker.io.mem.get := regNextDelay(mem, memDelay)
    }
    // csr
    val csr = Wire(CSR())
    csr := DontCare
    BoringUtils.addSink(csr, uniqueIdCSR)
    checker.io.result.csr := csr
  }
}

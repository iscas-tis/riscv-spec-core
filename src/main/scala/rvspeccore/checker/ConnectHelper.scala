package rvspeccore.checker

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rvspeccore.core.RVConfig
import rvspeccore.core.spec.instset.csr.CSR
import rvspeccore.core.spec.instset.csr.EventSig
import rvspeccore.core.tool.TLBMemInfo
import rvspeccore.core.tool.TLBSig
abstract class ConnectHelper {}

/** Connect RegFile to io.result.reg by BoringUtils
  */
object ConnectCheckerResult extends ConnectHelper {
  val uniqueIdReg: String   = "ConnectCheckerResult_UniqueIdReg"
  val uniqueIdMem: String   = "ConnectCheckerResult_UniqueIdMem"
  val uniqueIdCSR: String   = "ConnectCheckerResult_UniqueIdCSR"
  val uniqueIdEvent: String = "ConnectCheckerResult_UniqueIdEvent"
  val uniqueIdDTLB: String  = "ConnectCheckerResult_UniqueIdDTLB"
  val uniqueIdITLB: String  = "ConnectCheckerResult_UniqueIdITLB"

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
  def makeTLBSource(isDTLB: Boolean)(implicit XLEN: Int): TLBSig = {
    val tlbmem = Wire(new TLBSig())
    tlbmem.read.valid     := false.B
    tlbmem.read.addr      := 0.U
    tlbmem.read.data      := 0.U
    tlbmem.read.memWidth  := 0.U
    tlbmem.read.access    := false.B
    tlbmem.read.level     := 0.U
    tlbmem.write.valid    := false.B
    tlbmem.write.addr     := 0.U
    tlbmem.write.data     := 0.U
    tlbmem.write.memWidth := 0.U
    tlbmem.write.access   := false.B
    tlbmem.write.level    := 0.U

    if (isDTLB) {
      BoringUtils.addSource(tlbmem, uniqueIdDTLB)
    } else {
      BoringUtils.addSource(tlbmem, uniqueIdITLB)
    }
    tlbmem
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

  def makeEventSource()(implicit XLEN: Int, config: RVConfig): EventSig = {
    val event = Wire(new EventSig())
    event := DontCare
    BoringUtils.addSource(event, uniqueIdEvent)
    event
  }

  def setChecker(
      checker: CheckerWithResult,
      memDelay: Int = 0
  )(implicit XLEN: Int, config: RVConfig) = {
    // reg
    if (config.formal.arbitraryRegFile) ArbitraryRegFile.init

    val regVec = Wire(Vec(32, UInt(XLEN.W)))
    regVec := DontCare
    BoringUtils.addSink(regVec, uniqueIdReg)

    checker.io.result.reg := regVec
    checker.io.result.pc  := DontCare

    checker.io.result.internal := DontCare

    if (checker.io.mem != None) {
      val mem     = Wire(new MemSig)
      val dtlbmem = Wire(new TLBSig)
      val itlbmem = Wire(new TLBSig)
      mem     := DontCare
      dtlbmem := DontCare
      itlbmem := DontCare
      BoringUtils.addSink(mem, uniqueIdMem)
      BoringUtils.addSink(dtlbmem, uniqueIdDTLB)
      BoringUtils.addSink(itlbmem, uniqueIdITLB)
      checker.io.dtlbmem.get := dtlbmem
      checker.io.itlbmem.get := itlbmem
      checker.io.mem.get     := regNextDelay(mem, memDelay)
      // expose the signal below
      // assert(RegNext(checker.io.dtlbmem.get.read.valid, false.B) === false.B)
      // assert(RegNext(dtlbmem.read.valid, false.B) === false.B)
      // assert(RegNext(dtlbmem.read.addr, 0.U) === 0.U)
      // assert(RegNext(dtlbmem.read.data, 0.U) === 0.U)
    }
    // csr
    val csr = Wire(CSR())
    csr := DontCare
    BoringUtils.addSink(csr, uniqueIdCSR)
    checker.io.result.csr := csr

    val event = Wire(new EventSig())
    event := DontCare
    BoringUtils.addSink(event, uniqueIdEvent)
    checker.io.event := event
  }
}

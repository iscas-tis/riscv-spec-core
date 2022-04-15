package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.RVConfig

case class CSRInfo(addr: UInt, width: Option[Int], softwareWritable: Boolean) {
  def makeUInt(implicit XLEN: Int) = width match {
    case Some(value) => UInt(value.W)
    case None        => UInt(XLEN.W)
  }
}

object CSRInfo {
  def apply(addrStr: String, width: Option[Int] = None, softwareWritable: Boolean = true): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), None, softwareWritable)
  }
}

/** All CSR informations
  *
  *   - riscv-privileged-20211203
  *
  * addr:
  *
  *   - Chapter 2: Control and Status Registers (CSRs)
  *   - 2.2 CSR Listing
  *     - Table 2.1 ~ 2.6
  *
  * width: The `xxx` CSR is a `xxx`-bit register
  *
  * softwareWritable: `xxx` is never written by the implementation, though it
  * may be explicitly written by software
  */
object CSRInfos {
  // - User Trap Setup ???????????
  val ustatus  = CSRInfo("h000") // TODO
  val utvec    = CSRInfo("h005") // TODO
  val uip      = CSRInfo("h044") // TODO
  val uie      = CSRInfo("h004") // TODO
  val uscratch = CSRInfo("h040") // TODO
  val uepc     = CSRInfo("h041") // TODO
  val ucause   = CSRInfo("h042") // TODO
  val utval    = CSRInfo("h043") // TODO

  // - Unprivileged Floating-Point CSRs
  // - Unprivileged Counter/Timers

  // - Supervisor Trap Setup
  val sstatus    = CSRInfo("h100") // TODO
  val sie        = CSRInfo("h104") // TODO
  val stvec      = CSRInfo("h105") // TODO
  val scounteren = CSRInfo("h106") // TODO
  // - Supervisor Configuration
  // senvcfg
  // - Supervisor Trap Handling
  val sscratch = CSRInfo("h140") // TODO
  val sepc     = CSRInfo("h141") // TODO
  val scause   = CSRInfo("h142") // TODO
  val stval    = CSRInfo("h143") // TODO
  val sip      = CSRInfo("h144") // TODO
  // - Supervisor Trap Handling
  val satp = CSRInfo("h180") // TODO
  // - Debug/Trace Registers
  // scontext
  // what????????????????????????????????????????????
  val sedeleg = CSRInfo("h102") // TODO
  val sideleg = CSRInfo("h103") // TODO

  // - Hypervisor Trap Setup
  // - ...
  // - Virtual Supervisor Registers

  // - Machine Information Registers
  val mvendorid = CSRInfo("hf11") // TODO
  val marchid   = CSRInfo("hf12") // TODO
  val mimpid    = CSRInfo("hf13") // TODO
  val mhartid   = CSRInfo("hf14") // TODO
  // mconfigptr
  // - Machine Information Registers
  val mstatus    = CSRInfo("h300") // TODO
  val misa       = CSRInfo("h301") // TODO
  val medeleg    = CSRInfo("h302") // TODO
  val mideleg    = CSRInfo("h303") // TODO
  val mie        = CSRInfo("h304") // TODO
  val mtvec      = CSRInfo("h305") // TODO
  val mcounteren = CSRInfo("h306") // TODO
  // mstatush
  // - Machine Trap Handling
  val mscratch = CSRInfo("h340") // TODO
  val mepc     = CSRInfo("h341", None, false)
  val mcause   = CSRInfo("h342") // TODO
  val mtval    = CSRInfo("h343")
  val mip      = CSRInfo("h344") // TODO
  // mtinst
  // mtval2
  // - Machine Trap Handling
  // - ...
  // - Debug Mode Registers
}

class CSR()(implicit XLEN: Int) extends Bundle {
  val misa   = CSRInfos.misa.makeUInt
  val mtvec  = CSRInfos.mtvec.makeUInt
  val mepc   = CSRInfos.mepc.makeUInt
  val mcause = CSRInfos.mcause.makeUInt
  val mtval  = CSRInfos.mtval.makeUInt

  val table = List(
    (CSRInfos.misa,   misa),
    (CSRInfos.mtvec,  mtvec),
    (CSRInfos.mepc,   mepc),
    (CSRInfos.mcause, mcause),
    (CSRInfos.mtval,  mtval)
  )

  val MXLEN  = UInt(8.W)
  val IALIGN = UInt(8.W) // : the instruction-address alignment constraint the implementation enforces
  val ILEN   = UInt(8.W) // : the maximum instruction length supported by an implementation
}
object CSR {
  def apply()(implicit XLEN: Int): CSR = new CSR
  def wireInit()(implicit XLEN: Int, config: RVConfig): CSR = {
    val csr = Wire(new CSR)
    csr.misa   := 0.U
    csr.mtvec  := 0.U
    csr.mepc   := 0.U
    csr.mcause := 0.U
    csr.mtval  := 0.U

    csr.MXLEN := XLEN.U
    csr.IALIGN := {
      if (config.C) 16.U
      else 32.U
    }
    csr.ILEN := 32.U

    csr
  }
}

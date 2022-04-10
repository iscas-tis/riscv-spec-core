package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

case class CSRInfo(addr: UInt, width: Option[Int]) {
  def makeUInt()(implicit XLEN: Int) = width match {
    case Some(value) => UInt(value.W)
    case max_xlen    => UInt(XLEN.W)
  }
}

object CSRInfo {
  def apply(addrStr: String, width: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), Some(width))
  }
  def apply(addrStr: String, width: Option[Int] = None): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width)
  }
}

object CSRInfos {
  private val max_xlen = 64
  // - User Trap Setup ???????????
  val ustatus  = CSRInfo("h000", max_xlen)
  val utvec    = CSRInfo("h005", max_xlen)
  val uip      = CSRInfo("h044", max_xlen)
  val uie      = CSRInfo("h004", max_xlen)
  val uscratch = CSRInfo("h040", max_xlen)
  val uepc     = CSRInfo("h041", max_xlen)
  val ucause   = CSRInfo("h042", max_xlen)
  val utval    = CSRInfo("h043", max_xlen)

  // - Unprivileged Floating-Point CSRs
  // - Unprivileged Counter/Timers

  // - Supervisor Trap Setup
  val sstatus    = CSRInfo("h100", max_xlen)
  val sie        = CSRInfo("h104", max_xlen)
  val stvec      = CSRInfo("h105", max_xlen)
  val scounteren = CSRInfo("h106", max_xlen)
  // - Supervisor Configuration
  // senvcfg
  // - Supervisor Trap Handling
  val sscratch = CSRInfo("h140", max_xlen)
  val sepc     = CSRInfo("h141", max_xlen)
  val scause   = CSRInfo("h142", max_xlen)
  val stval    = CSRInfo("h143", max_xlen)
  val sip      = CSRInfo("h144", max_xlen)
  // - Supervisor Trap Handling
  val satp = CSRInfo("h180", max_xlen)
  // - Debug/Trace Registers
  // scontext
  // what????????????????????????????????????????????
  val sedeleg = CSRInfo("h102", max_xlen)
  val sideleg = CSRInfo("h103", max_xlen)

  // - Hypervisor Trap Setup
  // - ...
  // - Virtual Supervisor Registers

  // - Machine Information Registers
  val mvendorid = CSRInfo("hf11", max_xlen)
  val marchid   = CSRInfo("hf12", max_xlen)
  val mimpid    = CSRInfo("hf13", max_xlen)
  val mhartid   = CSRInfo("hf14", max_xlen)
  // mconfigptr
  // - Machine Information Registers
  val mstatus    = CSRInfo("h300", max_xlen)
  val misa       = CSRInfo("h301", max_xlen)
  val medeleg    = CSRInfo("h302", max_xlen)
  val mideleg    = CSRInfo("h303", max_xlen)
  val mie        = CSRInfo("h304", max_xlen)
  val mtvec      = CSRInfo("h305", max_xlen)
  val mcounteren = CSRInfo("h306", 32)
  // mstatush
  // - Machine Trap Handling
  val mscratch = CSRInfo("h340", max_xlen)
  val mepc     = CSRInfo("h341", max_xlen)
  val mcause   = CSRInfo("h342", max_xlen)
  val mtval    = CSRInfo("h343", max_xlen)
  val mip      = CSRInfo("h344", max_xlen)
  // mtinst
  // mtval2
  // - Machine Trap Handling
  // - ...
  // - Debug Mode Registers
}

class CSR()(implicit XLEN: Int) extends Bundle {
  val misa  = CSRInfos.misa.makeUInt()
  val mtvec = UInt(XLEN.W)

  val MXLEN = UInt(8.W) // 128 max

  val table = List(
    (CSRInfos.misa,  misa),
    (CSRInfos.mtvec, mtvec)
  )
}
object CSR {
  def apply()(implicit XLEN: Int): CSR = new CSR
  def init()(implicit XLEN: Int): CSR = {
    val csr = Wire(new CSR)
    csr.mtvec := 0.U
    csr
  }
}

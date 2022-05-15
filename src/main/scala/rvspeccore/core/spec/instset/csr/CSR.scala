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
  val mstatus    = CSRInfo("h300")
  val misa       = CSRInfo("h301") // TODO
  val medeleg    = CSRInfo("h302") // TODO
  val mideleg    = CSRInfo("h303") // TODO
  val mie        = CSRInfo("h304") // TODO
  val mtvec      = CSRInfo("h305")
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

case class CSRInfoSignal(info: CSRInfo, signal: UInt)

class CSR()(implicit XLEN: Int) extends Bundle with IgnoreSeqInBundle {
  val mstatus = CSRInfos.mstatus.makeUInt
  val misa    = CSRInfos.misa.makeUInt
  val mtvec   = CSRInfos.mtvec.makeUInt
  val mepc    = CSRInfos.mepc.makeUInt
  val mcause  = CSRInfos.mcause.makeUInt
  val mtval   = CSRInfos.mtval.makeUInt

  /** Table for all CSR signals in this Bundle
    */
  val table = List(
    CSRInfoSignal(CSRInfos.misa,   misa),
    CSRInfoSignal(CSRInfos.mtvec,  mtvec),
    CSRInfoSignal(CSRInfos.mepc,   mepc),
    CSRInfoSignal(CSRInfos.mcause, mcause),
    CSRInfoSignal(CSRInfos.mtval,  mtval)
  )

  val MXLEN  = UInt(8.W)
  val SXLEN  = UInt(8.W)
  val UXLEN  = UInt(8.W)
  val IALIGN = UInt(8.W) // : the instruction-address alignment constraint the implementation enforces
  val ILEN   = UInt(8.W) // : the maximum instruction length supported by an implementation

  val privilegeLevel = UInt(2.W)

  /** Table for all environment variable in this Bundle
    *
    * These environment variables may be changed when CSR changed.
    */
  val vTable = List(
    MXLEN,
    SXLEN,
    UXLEN,
    IALIGN,
    ILEN,
    privilegeLevel
  )

  // unpack mstatus
  val unMstatus = new MstatusStruct(mstatus)
}
object CSR {
  def apply()(implicit XLEN: Int): CSR = new CSR
  def wireInit()(implicit XLEN: Int, config: RVConfig): CSR = {
    val csr = Wire(new CSR)
    csr.mstatus := 0.U // TODO: set right init value
    csr.misa    := 0.U
    csr.mtvec   := 0.U
    csr.mepc    := 0.U
    csr.mcause  := 0.U
    csr.mtval   := 0.U

    csr.MXLEN := XLEN.U
    csr.SXLEN := XLEN.U
    csr.UXLEN := XLEN.U
    csr.IALIGN := {
      if (config.C) 16.U
      else 32.U
    }
    csr.ILEN := 32.U

    csr.privilegeLevel := PrivilegeLevel.M

    csr
  }
}

object PrivilegeLevel {
  val U        = 0.U(2.W)
  val S        = 1.U(2.W)
  val Reserved = 2.U(2.W)
  val M        = 3.U(2.W)
}

/** unpack mstatus with these method
  *
  * Some method with `require` means this field should be use under the
  * requirement.
  *
  *   - riscv-privileged-20211203
  *   - Chapter 3: Machine-Level ISA, Version 1.12
  *   - 3.1 Machine-Level CSRs
  *   - 3.1.6 Machine Status Registers (mstatus and mstatush)
  *     - Figure 3.6: Machine-mode status register (mstatus) for RV32.
  *     - Figure 3.7: Machine-mode status register (mstatus) for RV64.
  */
class MstatusStruct(mstatus: UInt) {
  // common
  // 31 or 63
  def SD(MXLEN: Int) = MXLEN match {
    case 32 => mstatus(31)
    case 64 => mstatus(63)
  }

  // RV32
  // 30 - 23
  def WPRI_30_23(MXLEN: Int) = { if (MXLEN == 32) mstatus(30, 23) else 0.U }

  // RV64
  // 62 - 23
  def WPRI_62_38(MXLEN: Int) = { if (MXLEN == 64) mstatus(62, 38) else 0.U }
  def MBE(MXLEN: Int)        = { if (MXLEN == 64) mstatus(37) else 0.U }
  def SBE(MXLEN: Int)        = { if (MXLEN == 64) mstatus(36) else 0.U }
  def SXL(MXLEN: Int)        = { if (MXLEN == 64) mstatus(35, 34) else 0.U }
  def UXL(MXLEN: Int)        = { if (MXLEN == 64) mstatus(33, 32) else 0.U }
  def WPRI_31_23(MXLEN: Int) = { if (MXLEN == 64) mstatus(31, 23) else 0.U }

  // common
  // 22 - 17
  def TSR  = mstatus(22)
  def TW   = mstatus(21)
  def TVM  = mstatus(20)
  def MXR  = mstatus(19)
  def SUM  = mstatus(18)
  def MPRV = mstatus(17)
  // 16 - 9
  def XS  = mstatus(16, 15)
  def FS  = mstatus(14, 13)
  def MPP = mstatus(12, 11)
  def VS  = mstatus(10, 9)
  // 8 - 0
  def SPP    = mstatus(8)
  def MPIE   = mstatus(7)
  def UBE    = mstatus(6)
  def SPIE   = mstatus(5)
  def WPRI_4 = mstatus(4)
  def MIE    = mstatus(3)
  def WPRI_2 = mstatus(2)
  def SIE    = mstatus(1)
  def WPRI_0 = mstatus(0)
}

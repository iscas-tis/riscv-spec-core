package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.RVConfig

case class CSRInfo(
    addr: UInt,
    width: Option[Int],
    wfn: Option[Int => UInt => UInt],
    rmask: Int => UInt,
    wmask: Int => UInt
) {
  def makeUInt(implicit XLEN: Int) = width match {
    case Some(value) => UInt(value.W)
    case None        => UInt(XLEN.W)
  }
}

object CSRInfo {
  def apply(
      addrStr: String,
      width: Option[Int] = None,
      wfn: Option[Int => UInt => UInt] = Some(_ => x => x),
      rmask: Int => UInt = XLEN => Fill(XLEN, 1.U(1.W)),
      wmask: Int => UInt = XLEN => Fill(XLEN, 1.U(1.W))
  ): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width, wfn, rmask, wmask)
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
  */
trait CSRInfos {
  // SideEffect
  val mstatusUpdateSideEffect: Option[Int => UInt => UInt] = Some(implicit XLEN =>
    mstatus => {
      val mstatusOld = WireInit(mstatus.asTypeOf(new MstatusStruct))
      // mstatusOld.mpp := "b11".U
      // if (XLEN == 64){
      //   // FIXME: nutshell 认为u mode 的uxl为全0 存疑 暂时修改参考模型 使其不报错
      //   if(config.CSRMisaExtList.contains('S')){
      //     mstatusOld.sxl := "b10".U
      //   }
      //   if(config.CSRMisaExtList.contains('U')){
      //     mstatusOld.uxl := "b10".U
      //   }
      // }
      // FIXME: 临时mpp只能为M状态 之后要时刻保持其值为能够支持的状态
      // 需要读Config来继续进行 当前三个模式都有 所以这一行要注释掉
      val mstatusNew = Cat(mstatusOld.fs === "b11".U, mstatusOld.asUInt(XLEN - 2, 0))
      mstatusNew
    }
  )

  // Address Map
  // - User Trap Setup ???????????
  // User CSR has been delete in V20211203
  // val ustatus  = CSRInfo("h000") // TODO
  // val utvec    = CSRInfo("h005") // TODO
  // val uip      = CSRInfo("h044") // TODO
  // val uie      = CSRInfo("h004") // TODO
  // val uscratch = CSRInfo("h040") // TODO
  // val uepc     = CSRInfo("h041") // TODO
  // val ucause   = CSRInfo("h042") // TODO
  // val utval    = CSRInfo("h043") // TODO

  // - Unprivileged Floating-Point CSRs
  // - Unprivileged Counter/Timers

  // - Supervisor Trap Setup
  // Sstatus Write Mask
  // -------------------------------------------------------
  //    19           9   5     2
  // 0  1100 0000 0001 0010 0010
  // 0  c    0    1    2    2
  // -------------------------------------------------------
  // val sieMask = "h222".U & mideleg
  // val sipMask = "h222".U & mideleg
  // MaskedRegMap(Sstatus, mstatus, sstatusWmask, mstatusUpdateSideEffect, sstatusRmask),
  val sstatus = CSRInfo(
    "h100",
    None,
    mstatusUpdateSideEffect,
    XLEN => "hc6122".U(XLEN.W) | "h8000000300018000".U,
    XLEN => "hc6122".U(XLEN.W)
  ) // TODO

  // MaskedRegMap(Sie, mie, sieMask, MaskedRegMap.NoSideEffect, sieMask),
  val sie        = CSRInfo("h104", rmask = XLEN => "h222".U(XLEN.W), wmask = XLEN => "h222".U(XLEN.W)) // TODO
  val stvec      = CSRInfo("h105")                                                                     // TODO
  val scounteren = CSRInfo("h106")                                                                     // TODO
  // - Supervisor Configuration
  // senvcfg
  // - Supervisor Trap Handling
  val sscratch = CSRInfo("h140") // TODO
  val sepc     = CSRInfo("h141") // TODO
  val scause   = CSRInfo("h142") // TODO
  val stval    = CSRInfo("h143") // TODO

  // MaskedRegMap(Sip, mip.asUInt, sipMask, MaskedRegMap.Unwritable, sipMask),
  val sip = CSRInfo(
    "h144",
    rmask = XLEN => "h222".U(XLEN.W),
    wmask = XLEN => "h222".U(XLEN.W)
  ) // FIXME: h222 is a error impl 忘了为啥说是错误的了
  // - Supervisor Trap Handling
  val satp = CSRInfo("h180") // TODO
  // - Debug/Trace Registers
  // scontext
  val sedeleg = CSRInfo("h102") // TODO
  val sideleg = CSRInfo("h103") // TODO

  // - Hypervisor Trap Setup
  // - ...
  // - Virtual Supervisor Registers

  // - Machine Information Registers
  val mvendorid = CSRInfo("hf11", wfn = None)
  val marchid   = CSRInfo("hf12", wfn = None)
  val mimpid    = CSRInfo("hf13", wfn = None)
  val mhartid   = CSRInfo("hf14", wfn = None)
  // mconfigptr
  // - Machine Information Registers
  val mstatus = CSRInfo("h300", wfn = mstatusUpdateSideEffect) // TODO
  // val misa       = CSRInfo("h301", None, Fill(XLEN, 1.U(1.W)), null, 0.U(XLEN.W)) // UnwritableMask implement
  val misa       = CSRInfo("h301")
  val medeleg    = CSRInfo("h302", wmask = XLEN => "hbbff".U) // FIXME: NutShell: medeleg[11] is read-only zero
  val mideleg    = CSRInfo("h303", wmask = XLEN => "h222".U)  // FIXME: simple impl use nutshell write mask
  val mie        = CSRInfo("h304")                            // TODO
  val mtvec      = CSRInfo("h305")                            // TODO
  val mcounteren = CSRInfo("h306")                            // TODO
  val mstatush   = CSRInfo("h310")                            // TODO
  // mstatush
  // - Machine Trap Handling
  val mscratch = CSRInfo("h340")                           // TODO
  val mepc     = CSRInfo("h341")
  val mcause   = CSRInfo("h342")                           // TODO
  val mtval    = CSRInfo("h343")
  val mip      = CSRInfo("h344", wmask = XLEN => "h77f".U) // FIXME: same as nutshell for wmask
  val mtinst   = CSRInfo("h34A")
  val mtval2   = CSRInfo("h34B")

  // Machine Configuration
  val menvcfg  = CSRInfo("h30A")
  val menvcfgh = CSRInfo("h31A")
  val mseccfg  = CSRInfo("h747")
  val mseccfgh = CSRInfo("h757")
  // ...
  // Machine Counter/Timers
  val mcycle       = CSRInfo("hb00")
  val minstret     = CSRInfo("hb02")
  val mhpmcounter3 = CSRInfo("hb03")
  val mhpmcounter4 = CSRInfo("hb04")
  // .. TODO: for mhpmcounter3 ~ mhpmcounter31
  val mcycleh       = CSRInfo("hb80")
  val mhpmcounter3h = CSRInfo("hb82")
  // .. TODO: for mhpmcounter3h ~ mhpmcounter31h
  // new add
  val cycle   = CSRInfo("hc00")
  val time    = CSRInfo("hc01")
  val instret = CSRInfo("hc02")
  // mtinst
  // mtval2
  // - Machine Trap Handling
  // - ...
  // - Debug Mode Registers
  // Machine Memory Protection
  val pmpcfg0 = CSRInfo("h3A0")
  val pmpcfg1 = CSRInfo("h3A1")
  val pmpcfg2 = CSRInfo("h3A2")
  val pmpcfg3 = CSRInfo("h3A3")
  // TODO: Can modify
  val pmpaddr0 = CSRInfo("h3B0")
  val pmpaddr1 = CSRInfo("h3B1")
  val pmpaddr2 = CSRInfo("h3B2")
  val pmpaddr3 = CSRInfo("h3B3")

}

object CSRInfos extends CSRInfos

case class CSRInfoSignal(info: CSRInfo, signal: UInt)

class EventSig()(implicit XLEN: Int) extends Bundle {
  val valid         = Bool()
  val intrNO        = UInt(XLEN.W)
  val cause         = UInt(XLEN.W)
  val exceptionPC   = UInt(XLEN.W)
  val exceptionInst = UInt(XLEN.W)
}

class CSR()(implicit XLEN: Int, config: RVConfig) extends Bundle with IgnoreSeqInBundle {
  // make default value for registers
  val misa       = CSRInfos.misa.makeUInt
  val mvendorid  = CSRInfos.mvendorid.makeUInt
  val marchid    = CSRInfos.marchid.makeUInt
  val mimpid     = CSRInfos.mimpid.makeUInt
  val mhartid    = CSRInfos.mhartid.makeUInt
  val mstatus    = CSRInfos.mstatus.makeUInt
  val mstatush   = CSRInfos.mstatush.makeUInt
  val mscratch   = CSRInfos.mscratch.makeUInt
  val mtvec      = CSRInfos.mtvec.makeUInt
  val mcounteren = CSRInfos.mcounteren.makeUInt
  val medeleg    = CSRInfos.medeleg.makeUInt
  val mideleg    = CSRInfos.mideleg.makeUInt
  val mip        = CSRInfos.mip.makeUInt
  val mie        = CSRInfos.mie.makeUInt
  val mepc       = CSRInfos.mepc.makeUInt
  val mcause     = CSRInfos.mcause.makeUInt
  val mtval      = CSRInfos.mtval.makeUInt

  val cycle = CSRInfos.cycle.makeUInt

  val scounteren = CSRInfos.scounteren.makeUInt

  // if(config.S){
  val scause   = CSRInfos.scause.makeUInt
  val stvec    = CSRInfos.stvec.makeUInt
  val sepc     = CSRInfos.sepc.makeUInt
  val stval    = CSRInfos.stval.makeUInt
  val sscratch = CSRInfos.sscratch.makeUInt
  // Memory Protection
  val satp     = CSRInfos.satp.makeUInt
  val pmpcfg0  = CSRInfos.pmpcfg0.makeUInt
  val pmpcfg1  = CSRInfos.pmpcfg1.makeUInt
  val pmpcfg2  = CSRInfos.pmpcfg2.makeUInt
  val pmpcfg3  = CSRInfos.pmpcfg3.makeUInt
  val pmpaddr0 = CSRInfos.pmpaddr0.makeUInt
  val pmpaddr1 = CSRInfos.pmpaddr1.makeUInt
  val pmpaddr2 = CSRInfos.pmpaddr2.makeUInt
  val pmpaddr3 = CSRInfos.pmpaddr3.makeUInt
  // }
  // val time      = CSRInfos.time.makeUInt
  // val instret   = CSRInfos.instret.makeUInt

  /** Table for all CSR signals in this Bundle CSRs in this table can be read or
    * write
    */
  val table = {
    val table_M = List(
      CSRInfoSignal(CSRInfos.misa, misa),
      CSRInfoSignal(CSRInfos.mvendorid, mvendorid),
      CSRInfoSignal(CSRInfos.marchid, marchid),
      CSRInfoSignal(CSRInfos.mimpid, mimpid),
      CSRInfoSignal(CSRInfos.mhartid, mhartid),
      CSRInfoSignal(CSRInfos.mstatus, mstatus),
      // CSRInfoSignal(CSRInfos.mstatush,  mstatush),
      CSRInfoSignal(CSRInfos.mscratch, mscratch),
      CSRInfoSignal(CSRInfos.mtvec, mtvec),
      CSRInfoSignal(CSRInfos.mcounteren, mcounteren),
      CSRInfoSignal(CSRInfos.mip, mip),
      CSRInfoSignal(CSRInfos.mie, mie),
      CSRInfoSignal(CSRInfos.mepc, mepc),
      CSRInfoSignal(CSRInfos.mcause, mcause),
      CSRInfoSignal(CSRInfos.mtval, mtval)
      // CSRInfoSignal(CSRInfos.cycle,     cycle)
      // CSRInfoSignal(CSRInfos.time,      time),
      // CSRInfoSignal(CSRInfos.instret,   instret)
    )
    val table_S = List(
      // Ch3.1.8  In systems without S-mode, the medeleg and mideleg registers should not exist.
      CSRInfoSignal(CSRInfos.medeleg, medeleg),
      CSRInfoSignal(CSRInfos.mideleg, mideleg),
      CSRInfoSignal(CSRInfos.scounteren, scounteren),
      CSRInfoSignal(CSRInfos.scause, scause),
      CSRInfoSignal(CSRInfos.stvec, stvec),
      CSRInfoSignal(CSRInfos.sepc, sepc),
      CSRInfoSignal(CSRInfos.stval, stval),
      CSRInfoSignal(CSRInfos.sstatus, mstatus),
      CSRInfoSignal(CSRInfos.sie, mie),
      CSRInfoSignal(CSRInfos.sip, mip),
      CSRInfoSignal(CSRInfos.sscratch, sscratch),
      // Memory Protection
      CSRInfoSignal(CSRInfos.satp, satp),
      CSRInfoSignal(CSRInfos.pmpcfg0, pmpcfg0),
      CSRInfoSignal(CSRInfos.pmpcfg1, pmpcfg1),
      CSRInfoSignal(CSRInfos.pmpcfg2, pmpcfg2),
      CSRInfoSignal(CSRInfos.pmpcfg3, pmpcfg3),
      CSRInfoSignal(CSRInfos.pmpaddr0, pmpaddr0),
      CSRInfoSignal(CSRInfos.pmpaddr1, pmpaddr1),
      CSRInfoSignal(CSRInfos.pmpaddr2, pmpaddr2),
      CSRInfoSignal(CSRInfos.pmpaddr3, pmpaddr3)
    )

    table_M ++
      (if (config.extensions.S) table_S else List())
  }

  val MXLEN  = UInt(8.W)
  val IALIGN = UInt(8.W) // : the instruction-address alignment constraint the implementation enforces
  val ILEN   = UInt(8.W) // : the maximum instruction length supported by an implementation

  /** Table for all environment variable in this Bundle
    *
    * These environment variables may be changed when CSR changed.
    */
  val vTable = List(
    MXLEN,
    IALIGN,
    ILEN
  )
}
object CSR {
  def apply()(implicit XLEN: Int, config: RVConfig): CSR = new CSR()
  def getMisaExt(ext: Char): UInt                        = { 1.U << (ext.toInt - 'A'.toInt) }
  def getMisaExtInt(ext: Char): Int                      = { (ext.toInt - 'A'.toInt) }
  def wireInit()(implicit XLEN: Int, config: RVConfig): CSR = {

    // TODO: finish the sideEffect func
    // Initial the value of CSR Regs

    // TODO: End
    // Set initial value to CSRs
    // CSR Class is just a Bundle, need to transfer to Wire
    val csr = Wire(new CSR())

    // Misa Initial Begin -----------------
    def getMisaMxl(): UInt = {
      XLEN match {
        case 32  => 1.U << (XLEN - 2)
        case 64  => 2.U << (XLEN - 2)
        case 128 => 3.U << (XLEN - 2)
      }
    }
    val misaInitVal =
      getMisaMxl() | config.csr.MisaExtList.foldLeft(0.U)((sum, i) => sum | getMisaExt(i)) // "h8000000000141105".U
    // val valid = csr.io.in.valid
    csr.misa := misaInitVal
    // Misa Initial End -----------------

    // mvendorid value 0 means non-commercial implementation
    csr.mvendorid := 0.U
    // marchid allocated globally by RISC-V International 0 means not implementation
    csr.marchid := 0.U
    // mimpid 0 means not implementation
    csr.mimpid  := 0.U
    csr.mhartid := 0.U
    csr.mstatus := config.initValue.getOrElse("mstatus", "h0000_1800").U
    val mstatusStruct = csr.mstatus.asTypeOf(new MstatusStruct)
    // val mstatus_change = csr.mstatus.asTypeOf(new MstatusStruct)
    // printf("mpp---------------:%b\n",mstatus_change.mpp)
    csr.mstatush   := 0.U // 310
    csr.mscratch   := 0.U
    csr.mtvec      := config.initValue.getOrElse("mtvec", "h0").U
    csr.mcounteren := 0.U
    csr.medeleg    := 0.U // 302
    csr.mideleg    := 0.U // 303
    csr.mip        := 0.U // 344
    csr.mie        := 0.U // 304
    csr.mepc       := 0.U
    csr.mcause     := 0.U
    csr.mtval      := 0.U
    csr.cycle      := 0.U // Warn TODO: NutShell not implemented

    // TODO: S Mode modify (if case)
    csr.scause     := 0.U
    csr.scounteren := 0.U // TODO: Need to modify
    csr.stvec      := 0.U
    csr.sepc       := 0.U // TODO: Need to modify
    csr.stval      := 0.U
    csr.sscratch   := 0.U
    // Memory Protection
    csr.satp := 0.U
    // // for test in NutShell
    // // TODO: need a correct if condition
    // if(XLEN == 64){
    //   csr.satp      :="h8000000000080002".U
    // }else{
    //   csr.satp      := 0.U
    // }
    csr.pmpcfg0  := 0.U
    csr.pmpcfg1  := 0.U
    csr.pmpcfg2  := 0.U
    csr.pmpcfg3  := 0.U
    csr.pmpaddr0 := 0.U
    csr.pmpaddr1 := 0.U
    csr.pmpaddr2 := 0.U
    csr.pmpaddr3 := 0.U
    // // TODO: Need Merge
    // val mstatus = RegInit("ha00002000".U(XLEN.W))
    // val mie = RegInit(0.U(XLEN.W))
    // // TODO: Need Merge End
    csr.MXLEN := XLEN.U
    csr.IALIGN := {
      if (config.extensions.C) 16.U
      else 32.U
    }
    csr.ILEN := 32.U
    csr
  }
}

// // TODO: WARL and ....

// object mtvec{
//   def apply()(implicit XLEN: Int): CSR = new CSR
//   def wireInit()(implicit XLEN: Int, config: RVConfig): CSR = {
//     // Volume II Page 29 3.1.7
//     // Value 0: Direct
//     // Value 1: Vectored
//     // Value >=2: Reserved
//     val reg_value  = UInt(0.W)
//   }
// }
// Level | Encoding |       Name       | Abbreviation
//   0   |    00    | User/Application |      U
//   1   |    01    |    Supervisor    |      S
//   2   |    10    |     Reserved     |
//   3   |    11    |     Machine      |      M
class MstatusStruct(implicit XLEN: Int) extends Bundle {
  // 记录Mstatus寄存器的状态 并使用Bundle按序构造寄存器
  val sd   = Output(UInt(1.W))
  val pad1 = if (XLEN == 64) Output(UInt(25.W)) else null
  val mbe  = if (XLEN == 64) Output(UInt(1.W)) else null
  val sbe  = if (XLEN == 64) Output(UInt(1.W)) else null
  val sxl  = if (XLEN == 64) Output(UInt(2.W)) else null
  val uxl  = if (XLEN == 64) Output(UInt(2.W)) else null
  val pad0 = if (XLEN == 64) Output(UInt(9.W)) else Output(UInt(8.W))
  val tsr  = Output(UInt(1.W)) // 22
  val tw   = Output(UInt(1.W)) // 21
  val tvm  = Output(UInt(1.W)) // 20
  val mxr  = Output(UInt(1.W)) // 19
  val sum  = Output(UInt(1.W)) // 18
  val mprv = Output(UInt(1.W)) // 17
  val xs   = Output(UInt(2.W)) // 16 ~ 15
  val fs   = Output(UInt(2.W)) // 14 ~ 13
  val mpp  = Output(UInt(2.W)) // 12 ~ 11
  val vs   = Output(UInt(2.W)) // 10 ~ 9
  val spp  = Output(UInt(1.W)) // 8
  val mpie = Output(UInt(1.W)) // 7
  val ube  = Output(UInt(1.W)) // 6
  val spie = Output(UInt(1.W)) // 5
  val pad2 = Output(UInt(1.W)) // 4
  val mie  = Output(UInt(1.W)) // 3
  val pad3 = Output(UInt(1.W)) // 2
  val sie  = Output(UInt(1.W)) // 1
  val pad4 = Output(UInt(1.W)) // 0
}
class SatpStruct(implicit XLEN: Int) extends Bundle {
  val mode = if (XLEN == 32) UInt(1.W) else UInt(4.W)
  val asid = if (XLEN == 32) UInt(9.W) else UInt(16.W)
  val ppn  = if (XLEN == 32) UInt(22.W) else UInt(44.W)
}

class SV39PTE() extends Bundle {
  val reserved = UInt(10.W)
  val ppn      = UInt(44.W)
  val rsw      = UInt(2.W)
  val flag     = UInt(8.W)
}

class PTEFlag() extends Bundle {
  val d = Bool()
  val a = Bool()
  val g = Bool()
  val u = Bool()
  val x = Bool()
  val w = Bool()
  val r = Bool()
  val v = Bool()
}

// TODO: FIXME: Merge to ours tools csr
// NutShell
// io.imemMMU.privilegeMode := privilegeMode
// io.dmemMMU.privilegeMode := Mux(mstatusStruct.mprv.asBool, mstatusStruct.mpp, privilegeMode)
// XiangShan
// tlbBundle.priv.imode := privilegeMode
// tlbBundle.priv.dmode := Mux(debugMode && dcsr.asTypeOf(new DcsrStruct).mprven, ModeM, Mux(mstatusStruct.mprv.asBool, mstatusStruct.mpp, privilegeMode))
// 当前还没有Debug Mode 因此按照NutShell 来讲 我认为是一致的

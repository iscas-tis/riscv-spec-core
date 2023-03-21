package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.RVConfig

case class CSRInfo(addr: UInt, width: Option[Int], rmask: UInt, wfn: UInt => UInt, wmask: UInt) {
  def makeUInt(implicit XLEN: Int) = width match {
    case Some(value) => UInt(value.W)
    case None        => UInt(XLEN.W)
  }
}

object CSRInfo {
  // [last three] rmask: UInt, wfn: UInt => UInt,  wmask: UInt
  def apply(addrStr: String)(implicit XLEN: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), None, Fill(XLEN, 1.U(1.W)), x=>x, Fill(XLEN, 1.U(1.W)))
  }
  def apply(addrStr: String, width: Option[Int])(implicit XLEN: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width, Fill(XLEN, 1.U(1.W)), x=>x, Fill(XLEN, 1.U(1.W)))
  }
  def apply(addrStr: String, width: Option[Int], rmask: UInt)(implicit XLEN: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width, rmask, x=>x, Fill(XLEN, 1.U(1.W)))
  }
  def apply(addrStr: String, width: Option[Int], rmask: UInt, wfn: UInt => UInt)(implicit XLEN: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width, rmask, wfn, Fill(XLEN, 1.U(1.W)))
  }
  def apply(addrStr: String, width: Option[Int], rmask: UInt, wfn: UInt => UInt, wmask: UInt)(implicit XLEN: Int): CSRInfo = {
    new CSRInfo(addrStr.U(12.W), width, rmask, wfn, wmask)
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
  */
class CSRInfos()(implicit XLEN: Int){
  // SideEffect
  def mstatusUpdateSideEffect(mstatus: UInt): UInt = {
    val mstatusOld = WireInit(mstatus.asTypeOf(new MstatusStruct))
    val mstatusNew = Cat(mstatusOld.fs === "b11".U, mstatus(XLEN-2, 0))
    mstatusNew
  }
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
  val mstatus    = CSRInfo("h300", None, Fill(XLEN, 1.U(1.W)), mstatusUpdateSideEffect) // TODO
  val misa       = CSRInfo("h301") // TODO
  val medeleg    = CSRInfo("h302") // TODO
  val mideleg    = CSRInfo("h303") // TODO
  val mie        = CSRInfo("h304") // TODO
  val mtvec      = CSRInfo("h305") // TODO
  val mcounteren = CSRInfo("h306") // TODO
  val mstatush   = CSRInfo("h310") // TODO
  // mstatush
  // - Machine Trap Handling
  val mscratch = CSRInfo("h340") // TODO
  val mepc     = CSRInfo("h341")
  val mcause   = CSRInfo("h342") // TODO
  val mtval    = CSRInfo("h343")
  val mip      = CSRInfo("h344") // TODO
  val mtinst   = CSRInfo("h34A")
  val mtval2   = CSRInfo("h34B")

  // Machine Configuration
  val menvcfg  = CSRInfo("h30A")
  val menvcfgh = CSRInfo("h31A")
  val mseccfg  = CSRInfo("h747")
  val mseccfgh = CSRInfo("h757")
  // ...
  // Machine Counter/Timers
  val mcycle        = CSRInfo("hb00")
  val minstret      = CSRInfo("hb02")
  val mhpmcounter3  = CSRInfo("hb03")
  val mhpmcounter4  = CSRInfo("hb04")
  // .. TODO: for mhpmcounter3 ~ mhpmcounter31
  val mcycleh       = CSRInfo("hb80")
  val mhpmcounter3h = CSRInfo("hb82")
  // .. TODO: for mhpmcounter3h ~ mhpmcounter31h
  // new add
  val cycle         = CSRInfo("hc00")
  val time          = CSRInfo("hc01")
  val instret       = CSRInfo("hc02")
  // mtinst
  // mtval2
  // - Machine Trap Handling
  // - ...
  // - Debug Mode Registers
}

case class CSRInfoSignal(info: CSRInfo, signal: UInt)

class CSR()(implicit XLEN: Int) extends Bundle with IgnoreSeqInBundle {
  // make default value for registers
  val CSRInfos   = new CSRInfos()
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

  val cycle      = CSRInfos.cycle.makeUInt

  val scounteren = CSRInfos.scounteren.makeUInt
  val sepc       = CSRInfos.sepc.makeUInt
  // val time      = CSRInfos.time.makeUInt
  // val instret   = CSRInfos.instret.makeUInt

  /** Table for all CSR signals in this Bundle
   * CSRs in this table can be read or write
  */
  val table = List(
    CSRInfoSignal(CSRInfos.misa,      misa),
    CSRInfoSignal(CSRInfos.mvendorid, mvendorid),
    CSRInfoSignal(CSRInfos.marchid,   marchid),
    CSRInfoSignal(CSRInfos.mimpid,    mimpid),
    CSRInfoSignal(CSRInfos.mhartid,   mhartid),
    CSRInfoSignal(CSRInfos.mstatus,   mstatus),
    CSRInfoSignal(CSRInfos.mstatush,  mstatush),
    CSRInfoSignal(CSRInfos.mscratch,  mscratch),
    CSRInfoSignal(CSRInfos.mtvec,     mtvec),
    CSRInfoSignal(CSRInfos.mcounteren,mcounteren),
    CSRInfoSignal(CSRInfos.medeleg,   medeleg),
    CSRInfoSignal(CSRInfos.mideleg,   mideleg),
    CSRInfoSignal(CSRInfos.mip,       mip),
    CSRInfoSignal(CSRInfos.mie,       mie),
    CSRInfoSignal(CSRInfos.mepc,      mepc),
    CSRInfoSignal(CSRInfos.mcause,    mcause),
    CSRInfoSignal(CSRInfos.mtval,     mtval),
    CSRInfoSignal(CSRInfos.cycle,     cycle),
    CSRInfoSignal(CSRInfos.scounteren,scounteren),
    CSRInfoSignal(CSRInfos.sepc,      sepc)
    // CSRInfoSignal(CSRInfos.time,      time),
    // CSRInfoSignal(CSRInfos.instret,   instret)
  )

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
  def apply()(implicit XLEN: Int): CSR = new CSR()
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
        case 32  => 1.U << (XLEN-2)
        case 64  => 2.U << (XLEN-2)
        case 128 => 3.U << (XLEN-2)
      }
    }
    def getMisaExt(ext: Char): UInt = {1.U << (ext.toInt - 'A'.toInt)}
    val misaInitVal = getMisaMxl() | config.CSRMisaExtList.foldLeft(0.U)((sum, i) => sum | getMisaExt(i)) //"h8000000000141105".U 
    // val valid = csr.io.in.valid
    csr.misa      := misaInitVal
    // Misa Initial End -----------------
    
    // mvendorid value 0 means non-commercial implementation
    csr.mvendorid := 0.U
    // marchid allocated globally by RISC-V International 0 means not implementation
    csr.marchid   := 0.U
    // mimpid 0 means not implementation
    csr.mimpid    := 0.U
    csr.mhartid   := 0.U
    csr.mstatus   := zeroExt("h000000ff".U, XLEN)  //300
    val mstatusStruct = csr.mstatus.asTypeOf(new MstatusStruct)
    // val mstatus_change = csr.mstatus.asTypeOf(new MstatusStruct)
    // printf("mpp---------------:%b\n",mstatus_change.mpp)
    csr.mstatush  := 0.U //310
    // FIXME: Need to give mtvec a default BASE Addr and Mode
    csr.mscratch  := 0.U
    csr.mtvec     := 0.U
    csr.mcounteren:= 0.U
    csr.medeleg   := 0.U  //302
    csr.mideleg   := 0.U  //303
    csr.mip       := 0.U  //344
    csr.mie       := 0.U  //304
    csr.mepc      := 0.U
    csr.mcause    := 0.U
    csr.mtval     := 0.U
    csr.cycle     := 0.U // Warn TODO: NutShell not implemented
    csr.scounteren:= 0.U // TODO: Need to modify
    csr.sepc      := 0.U // TODO: Need to modify
    // // TODO: Need Merge
    // val mstatus = RegInit("ha00002000".U(XLEN.W))
    // val mie = RegInit(0.U(XLEN.W))

    // // TODO: Need Merge End
    csr.MXLEN := XLEN.U
    csr.IALIGN := {
      if (config.C) 16.U
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
  val mbe  = if (XLEN == 64) Output(UInt(1.W))  else null
  val sbe  = if (XLEN == 64) Output(UInt(1.W))  else null
  val sxl  = if (XLEN == 64) Output(UInt(2.W))  else null
  val uxl  = if (XLEN == 64) Output(UInt(2.W))  else null
  val pad0 = if (XLEN == 64) Output(UInt(9.W))  else Output(UInt(8.W))
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
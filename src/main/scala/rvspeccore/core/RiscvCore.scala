package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import spec.instset.csr.CSR
import spec.instset.csr.EventSig
import spec.instset.csr.SatpStruct

abstract class BaseCore()(implicit config: RVConfig) extends Module {
  // Define Basic parts
  implicit val XLEN: Int = config.XLEN
  val io = IO(new Bundle {
    // Processor IO
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())
    val mem   = new MemIO
//    val tlb   = new TLBIO
    // Exposed processor status
    val now      = Output(State())
    val next     = Output(State())
    val event    = Output(new EventSig)
    val iFetchpc = Output(UInt(XLEN.W))
  })
  // Initial State
  val now  = RegInit(State.wireInit())
  val next = Wire(State())
  val mem  = Wire(new MemIO)
//  val tlb  = Wire(new TLBIO)
  // Global Data
  val global_data = Wire(new GlobalData) // TODO: GlobalData only has setpc? event, iFetchpc?
  val event       = Wire(new EventSig)
  val iFetchpc    = Wire(UInt(XLEN.W))
}
class GlobalData extends Bundle {
  val setpc = Bool()
}
class ReadMemIO()(implicit XLEN: Int) extends Bundle {
  val valid    = Output(Bool())
  val addr     = Output(UInt(XLEN.W))
  val memWidth = Output(UInt(log2Ceil(XLEN + 1).W))
  val data     = Input(UInt(XLEN.W))
}

class WriteMemIO()(implicit XLEN: Int) extends Bundle {
  val valid    = Output(Bool())
  val addr     = Output(UInt(XLEN.W))
  val memWidth = Output(UInt(log2Ceil(XLEN + 1).W))
  val data     = Output(UInt(XLEN.W))
}

class MemIO()(implicit XLEN: Int) extends Bundle {
  val read  = new ReadMemIO
  val write = new WriteMemIO
}

//class TLBIO()(implicit XLEN: Int) extends Bundle {
//  val Anotherread  = Vec(3 + 3, new ReadMemIO())
//  val Anotherwrite = Vec(3, new WriteMemIO())
//}

class Internal() extends Bundle {
  val privilegeMode = UInt(2.W)
}
object Internal {
  def apply(): Internal = new Internal
  def wireInit(): Internal = {
    val internal = Wire(new Internal)
    internal.privilegeMode := 0x3.U
    internal
  }
}

class State()(implicit XLEN: Int, config: RVConfig) extends Bundle {
  val reg = Vec(32, UInt(XLEN.W))
  val pc  = UInt(XLEN.W)
  val csr = CSR()

  val internal = Internal()
}

object State {
  def apply()(implicit XLEN: Int, config: RVConfig): State = new State
  def wireInit(pcStr: String = "h0000_0200")(implicit XLEN: Int, config: RVConfig): State = {
    val state = Wire(new State)

    state.reg := Seq.fill(32)(0.U(XLEN.W))
    state.pc  := pcStr.U(XLEN.W)
    state.csr := CSR.wireInit()

    state.internal := Internal.wireInit()

    state
  }
}

class RiscvCore()(implicit config: RVConfig) extends BaseCore with RVInstSet {
  // Initial the value
  // these signals should keep the value in the next clock if there no changes below
  next              := now
  global_data.setpc := false.B
  event             := 0.U.asTypeOf(new EventSig)
  iFetchpc          := now.pc
  // dont read or write mem
  // if there no LOAD/STORE below
  mem := 0.U.asTypeOf(new MemIO)
//  tlb := 0.U.asTypeOf(new TLBIO)

  // ID & EXE
  when(io.valid) {
    // CSR
    // TODO: merge into a function?
    next.csr.cycle := now.csr.cycle + 1.U
    exceptionSupportInit()
//    val (resultStatus, resultPC) = if (XLEN == 32) (true.B, now.pc) else iFetchTrans(now.pc)
    inst := io.inst
//    when(resultStatus) {
//      inst := io.inst
//    }.otherwise {
//      inst := 0.U(XLEN.W) // With a NOP instruction
//    }
    iFetchpc := now.pc

    // Decode and Excute
    config.XLEN match {
      case 32 => {
        doRV32I
        if (config.M) { doRV32M }
        if (config.C) { doRV32C }
      }
      case 64 => {
        doRV64I
        if (config.M) { doRV64M }
        if (config.C) { doRV64C }
      }
    }
    // TODO: add config for privileged instruction
    doRVPrivileged()
    doRVZicsr
    doRVZifencei

    // End excute
    next.reg(0) := 0.U

    when(!global_data.setpc) {
      if (config.C) {
        // + 4.U for 32 bits width inst
        // + 2.U for 16 bits width inst in C extension
        next.pc := now.pc + Mux(inst(1, 0) === "b11".U, 4.U, 2.U)
      } else {
        next.pc := now.pc + 4.U
      }
    }

    tryRaiseException()
  }

  // mem port
  io.mem <> mem
//  io.tlb <> tlb

  // update
  now := next

  // output
  io.now      := now
  io.next     := next
  io.event    := event
  io.iFetchpc := iFetchpc
}

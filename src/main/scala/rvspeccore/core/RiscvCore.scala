package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import spec.instset.csr.CSR
import spec.instset.csr.EventSig
import spec.instset.csr.SatpStruct

import rvspeccore.checker.ArbitraryRegFile

abstract class BaseCore()(implicit val config: RVConfig) extends Module {
  implicit val XLEN: Int = config.XLEN

  // State
  val now  = Wire(State())
  val next = Wire(State())
  // IO ports
  val iFetchpc = Wire(UInt(XLEN.W))
  val mem      = Wire(new MemIO)
  val tlb      = if (config.functions.tlb) Some(Wire(new TLBIO)) else None
  // Global signals
  val inst        = Wire(UInt(32.W))
  val global_data = Wire(new GlobalData) // TODO: GlobalData only has setpc? event, iFetchpc?
  val event       = Wire(new EventSig)
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

class TLBIO()(implicit XLEN: Int) extends Bundle {
  val Anotherread  = Vec(3 + 3, new ReadMemIO())
  val Anotherwrite = Vec(3, new WriteMemIO())
}

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
  def wireInit()(implicit XLEN: Int, config: RVConfig): State = {
    val state = Wire(new State)

    state.reg := {
      if (config.formal.arbitraryRegFile) ArbitraryRegFile.gen
      else Seq.fill(32)(0.U(XLEN.W))
    }
    state.pc  := config.initValue.getOrElse("pc", "h8000_0000").U(XLEN.W)
    state.csr := CSR.wireInit()

    state.internal := Internal.wireInit()

    state
  }
}

class RiscvTrans()(implicit config: RVConfig) extends BaseCore with RVInstSet {
  val io = IO(new Bundle {
    // Processor IO
    val inst     = Input(UInt(32.W))
    val valid    = Input(Bool())
    val iFetchpc = Output(UInt(XLEN.W))
    val mem      = new MemIO
    val tlb      = if (config.functions.tlb) Some(new TLBIO) else None
    // Processor status
    val now  = Input(State())
    val next = Output(State())
    // Exposed signals
    val event = Output(new EventSig)
  })

  // Initial the value
  now := io.now
  // these signals should keep the value in the next clock if there no changes below
  next              := now
  inst              := 0.U
  global_data.setpc := false.B
  event             := 0.U.asTypeOf(new EventSig)
  iFetchpc          := now.pc

  // dont read or write mem
  // if there no LOAD/STORE below
  mem := 0.U.asTypeOf(new MemIO)
  tlb.map(_ := 0.U.asTypeOf(new TLBIO))

  // ID & EXE
  when(io.valid) {
    // CSR
    // TODO: merge into a function?
    next.csr.cycle := now.csr.cycle + 1.U
    exceptionSupportInit()

    if (!config.functions.tlb) {
      inst     := io.inst
      iFetchpc := now.pc
    } else {
      val (resultStatus, resultPC) = iFetchTrans(now.pc)
      inst     := Mux(resultStatus, io.inst, "h0000_0013".U) // With a NOP instruction
      iFetchpc := resultPC
    }

    // Decode and Excute
    doRVI
    if (config.extensions.C) doRVC
    if (config.extensions.M) doRVM
    if (config.functions.privileged) doRVPrivileged
    if (config.extensions.Zicsr) doRVZicsr
    if (config.extensions.Zifencei) doRVZifencei

    // End excute
    next.reg(0) := 0.U

    when(!global_data.setpc) {
      if (config.extensions.C) {
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
  io.tlb.map(_ <> tlb.get)

  io.next     := next
  io.event    := event
  io.iFetchpc := iFetchpc
}

class RiscvCore()(implicit config: RVConfig) extends Module {
  implicit val XLEN: Int = config.XLEN

  val io = IO(new Bundle {
    // Processor IO
    val inst     = Input(UInt(32.W))
    val valid    = Input(Bool())
    val iFetchpc = Output(UInt(XLEN.W))
    val mem      = new MemIO
    val tlb      = if (config.functions.tlb) Some(new TLBIO) else None
    // Processor status
    val now  = Output(State())
    val next = Output(State())
    // Exposed signals
    val event = Output(new EventSig)
  })

  val state = RegInit(State.wireInit())
  val trans = Module(new RiscvTrans())

  trans.io.inst  := io.inst
  trans.io.valid := io.valid
  trans.io.mem <> io.mem
  trans.io.tlb.map(_ <> io.tlb.get)

  trans.io.now := state
  state        := trans.io.next

  io.now      := state
  io.next     := trans.io.next
  io.event    := trans.io.event
  io.iFetchpc := trans.io.iFetchpc
}

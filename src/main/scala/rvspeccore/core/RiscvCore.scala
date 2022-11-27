package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import spec.instset.csr.CSR

abstract class BaseCore()(implicit config: RVConfig) extends Module {
  // Define Basic parts
  implicit val XLEN: Int = config.XLEN
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())
    val mem = new MemIO
    val now  = Output(State())
    val next = Output(State())
  })
  // Initial State
  val now  = RegInit(State.wireInit())
  val next = Wire(State())
  val mem = Wire(new MemIO)
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

class State()(implicit XLEN: Int) extends Bundle {
  val reg = Vec(32, UInt(XLEN.W))
  val pc  = UInt(XLEN.W)
  val csr = CSR()
}

object State {
  def apply()(implicit XLEN: Int): State = new State
  def wireInit(pcStr: String = "h8000_0000")(implicit XLEN: Int, config: RVConfig): State = {
    val state = Wire(new State)
    state.reg := Seq.fill(32)(0.U(XLEN.W))
    state.pc  := pcStr.U(XLEN.W)
    state.csr := CSR.wireInit()
    state
  }
}

class RiscvCore()(implicit config: RVConfig) extends BaseCore with RVInstSet {
  // should keep the value in the next clock
  // if there no changes below
  // Initial the value of next
  next := now

  // dont read or write mem
  // if there no LOAD/STORE below
  mem := 0.U.asTypeOf(new MemIO)

  // ID & EXE
  when(io.valid) {
    exceptionSupportInit()

    inst := io.inst
    // Decode and Excute
    // Attention: Config(_) "_" means Config(__have_some_value__)
    printf("Current Inst:%x\n",inst)
    config match {
      case RV32Config(_) => {
        doRV32I
        if (config.M) { doRV32M }
        if (config.C) { doRV32C }
      }
      case RV64Config(_) => {
        doRV64I
        if (config.M) { doRV64M }
        if (config.C) { doRV64C }
      }
    }
    // do without config for now
    doRVZicsr
    doRVZifencei

    next.reg(0) := 0.U

    when(!setPc) {
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

  // update
  now := next

  // output
  io.now  := now
  io.next := next
}

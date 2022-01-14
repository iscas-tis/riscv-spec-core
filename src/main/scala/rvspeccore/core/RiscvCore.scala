package rvspeccore.core

import chisel3._
import chisel3.util._

import spec.behavior._

abstract class BaseCore()(implicit config: RVConfig) extends Module {
  implicit val XLEN: Int = config.XLEN

  val now  = RegInit(State().init())
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

  def init(reg: UInt = 0.U(XLEN.W), pc: UInt = "h8000_0000".U(XLEN.W)): State = {
    val state = Wire(this)
    state.reg := Seq.fill(32)(reg)
    state.pc  := pc
    state
  }
}

object State {
  def apply()(implicit XLEN: Int): State = new State
}

class RiscvCore()(implicit config: RVConfig) extends BaseCore with Decode with Execute {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val mem = new MemIO

    val now  = Output(State())
    val next = Output(State())
  })

  // should keep the value in the next clock
  // if there no changes below
  next := now

  // dont read or write mem
  // if there no LOAD/STORE below
  mem := 0.U.asTypeOf(new MemIO)

  // ID & EXE
  when(io.valid) {
    inst := io.inst

    // Decode and Excute
    config match {
      case RV32Config() => {
        deRV32I
      }
      case RV64Config() => {
        deRV64I
        // do more if with extension
      }
    }

    when(!setPc) { next.pc := now.pc + 4.U }

    // riscv-spec-20191213
    // Register x0 is hardwired with all bits equal to 0.
    // Register x0 can be used as the destination if the result is not required.
    next.reg(0) := 0.U(XLEN.W)
  }

  // mem port
  io.mem <> mem

  // update
  now := next

  // output
  io.now  := now
  io.next := next
}

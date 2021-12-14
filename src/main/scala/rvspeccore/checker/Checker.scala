package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

abstract class Checker extends Module

/** Checker with state port.
  *
  * Link to the `CheckerCore` directly.
  *
  * @param gen
  */
class CheckerWithState[T <: RiscvCore](gen: => T) extends Checker {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val state = Input(State())
  })

  // link to checkerCore
  val checkerCore = Module(new CheckerCore(gen))
  checkerCore.io <> io
}

class WriteBack extends Bundle {
  val valid = Bool()
  val dest  = UInt(8.W)
  val data  = UInt(64.W)
}

/** Checker with write back port.
  *
  * Write back will work when `io.wb.valid` is true, But only check assert after
  * commit instruction with `io.valid`.
  *
  * @param gen
  *   Generator of a spec core
  */
class CheckerWithWB[T <: RiscvCore](gen: => T) extends Checker {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val pc = Input(UInt(64.W))
    val wb = Input(new WriteBack)
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  when(io.wb.valid) {
    reg(io.wb.dest) := io.wb.data
  }

  // link to checkerCore
  val checkerCore = Module(new CheckerCore(gen))
  checkerCore.io.inst      := io.inst
  checkerCore.io.valid     := io.valid
  checkerCore.io.state.reg := reg
  checkerCore.io.state.pc  := io.pc
}

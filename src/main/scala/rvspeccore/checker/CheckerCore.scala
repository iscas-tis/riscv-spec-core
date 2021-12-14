package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

/** Link design and spec core, set assertions
  *
  * Assert will be checked in the next clock after commit instruction
  *
  * @param gen
  *   Generator of a spec core
  */
class CheckerCore[T <: RiscvCore](gen: => T) extends Module {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())
    val state = Input(State())
  })

  // link to core
  val core = Module(gen)

  core.io.inst  := io.inst
  core.io.valid := io.valid

  // assert
  // commit inst in last clock, updated in this clock
  when(RegNext(io.valid)) {
    for (i <- 0 until 32) {
      assert(core.io.now.reg(i.U) === io.state.reg(i.U))
    }
    assert(core.io.now.pc === io.state.pc)
  }
}

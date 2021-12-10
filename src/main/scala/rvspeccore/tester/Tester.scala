package rvspeccore.tester

import chisel3._
import chisel3.util._

import rvspeccore.core._

class Tester[T <: RiscvCore](gen: => T = { new RiscvCore }) extends Module {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val state = Input(new State)
  })

  // link to core
  val core = Module(gen)

  core.io.inst  := io.inst
  core.io.valid := io.valid

  // assert
  for (i <- 0 until 32) {
    assert(core.io.now.reg(i.U) === io.state.reg(i.U))
  }
  assert(core.io.now.pc === io.state.pc)
}

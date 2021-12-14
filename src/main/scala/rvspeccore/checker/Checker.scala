package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

abstract class Checker extends Module

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

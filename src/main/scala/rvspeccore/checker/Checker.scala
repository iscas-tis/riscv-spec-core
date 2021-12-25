package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

abstract class Checker extends Module

class InstCommit extends Bundle {
  val valid = Bool()
  val inst  = UInt(32.W)
  val pc    = UInt(64.W)
}
object InstCommit {
  def apply() = new InstCommit
}

/** Checker with result port.
  *
  * Check pc of commited instruction, all register and next pc.
  *
  * @param gen
  */
class CheckerWithResult(gen: => RiscvCore) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val result     = Input(State())
  })

  // link to spec core
  val specCore = Module(gen)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst

  // assert in current clock
  when(io.instCommit.valid) {
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    for (i <- 0 until 32) {
      assert(io.result.reg(i.U) === specCore.io.next.reg(i.U))
    }
    // next pc
    assert(io.result.pc === specCore.io.next.pc)
  }
}

class WriteBack extends Bundle {
  val valid = Bool()
  val dest  = UInt(5.W)
  val data  = UInt(64.W)
}
object WriteBack {
  def apply() = new WriteBack
}

/** Checker with write back port.
  *
  * Check pc of commited instruction and the register been write back.
  *
  * @param gen
  *   Generator of a spec core
  */
class CheckerWithWB(gen: => RiscvCore) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val wb         = Input(WriteBack())
  })

  // link to spec core
  val specCore = Module(gen)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst

  // assert in current clock
  when(io.instCommit.valid) {
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    when(io.wb.valid) {
      assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
    }
    // next pc: no next pc signal in this case
  }
}

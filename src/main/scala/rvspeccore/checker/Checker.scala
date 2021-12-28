package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

abstract class Checker()(implicit config: RVConfig) extends Module {
  implicit val width: Int = config.width
}

class InstCommit()(implicit width: Int) extends Bundle {
  val valid = Bool()
  val inst  = UInt(32.W)
  val pc    = UInt(width.W)
}
object InstCommit {
  def apply()(implicit width: Int) = new InstCommit
}

/** Checker with result port.
  *
  * Check pc of commited instruction, all register and next pc.
  *
  * @param gen
  */
class CheckerWithResult()(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val result     = Input(State())
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
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

class WriteBack()(implicit width: Int) extends Bundle {
  val valid = Bool()
  val dest  = UInt(5.W)
  val data  = UInt(width.W)
}
object WriteBack {
  def apply()(implicit width: Int) = new WriteBack
}

/** Checker with write back port.
  *
  * Check pc of commited instruction and the register been write back.
  *
  * @param gen
  *   Generator of a spec core
  */
class CheckerWithWB()(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val wb         = Input(WriteBack())
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
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

package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._

abstract class Checker()(implicit config: RVConfig) extends Module {
  implicit val XLEN: Int = config.XLEN
}

class InstCommit()(implicit XLEN: Int) extends Bundle {
  val valid = Bool()
  val inst  = UInt(32.W)
  val pc    = UInt(XLEN.W)
}
object InstCommit {
  def apply()(implicit XLEN: Int) = new InstCommit
}

/** Checker with result port.
  *
  * Check pc of commited instruction, all register and next pc.
  *
  * @param gen
  */
class CheckerWithResult(checkMem: Boolean = true)(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val result     = Input(State())
    val mem        = if (checkMem) Some(Input(new MemIO)) else None
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst

  specCore.io.mem.read.data := { if (checkMem) io.mem.get.read.data else DontCare }

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

    if (checkMem) {
      assert(io.mem.get.read.valid === specCore.io.mem.read.valid)
      assert(io.mem.get.read.addr === specCore.io.mem.read.addr)
      assert(io.mem.get.read.memWidth === specCore.io.mem.read.memWidth)

      assert(io.mem.get.write.valid === specCore.io.mem.write.valid)
      assert(io.mem.get.write.addr === specCore.io.mem.write.addr)
      assert(io.mem.get.write.memWidth === specCore.io.mem.write.memWidth)
      assert(io.mem.get.write.data === specCore.io.mem.write.data)
    }
  }
}

class WriteBack()(implicit XLEN: Int) extends Bundle {
  val valid = Bool()
  val dest  = UInt(5.W)
  val data  = UInt(XLEN.W)
}
object WriteBack {
  def apply()(implicit XLEN: Int) = new WriteBack
}

/** Checker with write back port.
  *
  * Check pc of commited instruction and the register been write back.
  *
  * @param gen
  *   Generator of a spec core
  */
class CheckerWithWB(checkMem: Boolean = true)(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val wb         = Input(WriteBack())
    val mem        = if (checkMem) Some(Input(new MemIO)) else None
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst

  specCore.io.mem.read.data := { if (checkMem) io.mem.get.read.data else DontCare }

  // assert in current clock
  when(io.instCommit.valid) {
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    when(io.wb.valid) {
      assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
    }
    // next pc: no next pc signal in this case
    if (checkMem) {
      assert(io.mem.get.read.valid === specCore.io.mem.read.valid)
      assert(io.mem.get.read.addr === specCore.io.mem.read.addr)
      assert(io.mem.get.read.memWidth === specCore.io.mem.read.memWidth)

      assert(io.mem.get.write.valid === specCore.io.mem.write.valid)
      assert(io.mem.get.write.addr === specCore.io.mem.write.addr)
      assert(io.mem.get.write.memWidth === specCore.io.mem.write.memWidth)
      assert(io.mem.get.write.data === specCore.io.mem.write.data)
    }
  }
}

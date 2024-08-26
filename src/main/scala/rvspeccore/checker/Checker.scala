package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._
import rvspeccore.core.spec.instset.csr.EventSig

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
  * Check pc of commited instruction and next value of all register. Although
  * `pc` in the result port, but it won't be checked.
  */
class CheckerWithResult(checkMem: Boolean = true, enableReg: Boolean = false)(implicit config: RVConfig)
    extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val result     = Input(State())
    val mem        = if (checkMem) Some(Input(new MemIO)) else None
    val event      = Input(new EventSig())
  })
  // TODO: io.result has .internal states now, consider use it or not

  /** Delay input data by a register if `delay` is true.
    *
    * This function helps to get signal values from the counterexample that only
    * contains values of registers from model checking.
    */
  def regDelay[T <: Data](data: T): T = {
    if (enableReg) RegNext(data, 0.U.asTypeOf(data.cloneType)) else data
  }

  // link to spec core
  val specCore = Module(new RiscvCore)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst
  // assertions

  if (checkMem) {
      assert(regDelay(io.mem.get.read.valid) === regDelay(specCore.io.mem.read.valid))
      when(regDelay(io.mem.get.read.valid) || regDelay(specCore.io.mem.read.valid)){
        assert(regDelay(io.mem.get.read.addr) === regDelay(specCore.io.mem.read.addr))
        assert(regDelay(io.mem.get.read.memWidth) === regDelay(specCore.io.mem.read.memWidth))
      }
      assert(regDelay(io.mem.get.write.valid) === regDelay(specCore.io.mem.write.valid))
      when(regDelay(io.mem.get.write.valid) || regDelay(specCore.io.mem.write.valid)){
        assert(regDelay(io.mem.get.write.addr) === regDelay(specCore.io.mem.write.addr))
        assert(regDelay(io.mem.get.write.data) === regDelay(specCore.io.mem.write.data))
        assert(regDelay(io.mem.get.write.memWidth) === regDelay(specCore.io.mem.write.memWidth))
      }
      specCore.io.mem.read.data := io.mem.get.read.data
  } else {
    specCore.io.mem.read.data := DontCare
  }

  when(regDelay(io.instCommit.valid)) {
    // next reg
    for (i <- 0 until 32) {
      assert(regDelay(io.result.reg(i.U)) === regDelay(specCore.io.next.reg(i.U)))
    }
  }
  // printf("[SSD] io.instCommit.valid %x io.event.valid %x speccore.io.event.valid %x\n", io.instCommit.valid, io.event.valid, specCore.io.event.valid)
  when(regDelay(io.instCommit.valid)) {
    // now pc:
    assert(regDelay(io.instCommit.pc) === regDelay(specCore.io.now.pc))
    // next pc: hard to get next pc in a pipeline, check it at next instruction

    // next csr:
    // rvmini not change csr values
    // io.result.csr.table.zip(specCore.io.next.csr.table).map {
    //   case (result, next) => {
    //     assert(result.signal === next.signal)
    //   }
    // }
  }

  when(regDelay(io.event.valid) || regDelay(specCore.io.event.valid)) {
    assert(
      regDelay(io.event.valid) === regDelay(specCore.io.event.valid)
    ) // Make sure DUT and specCore currently occur the same exception
    assert(regDelay(io.event.intrNO) === regDelay(specCore.io.event.intrNO))
    assert(regDelay(io.event.cause) === regDelay(specCore.io.event.cause))
    assert(regDelay(io.event.exceptionPC) === regDelay(specCore.io.event.exceptionPC))
    assert(regDelay(io.event.exceptionInst) === regDelay(specCore.io.event.exceptionInst))
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

  val specCoreWBValid = WireInit(false.B)
  val specCoreWBDest  = WireInit(0.U(5.W))
  for (i <- 0 until 32) {
    when(specCore.io.now.reg(i) =/= specCore.io.next.reg(i)) {
      specCoreWBValid := true.B
      specCoreWBDest  := i.U
    }
  }

  // assert in current clock
  when(io.instCommit.valid) {
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    when(specCoreWBValid) {
      // prevent DUT not rise a write back
      assert(io.wb.dest === specCoreWBDest)
      assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
    }.otherwise {
      // DUT may try to write back to x0, but it should not take effect
      // if DUT dose write in x0, it will be check out at next instruction
      when(io.wb.valid && io.wb.dest =/= 0.U) {
        assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
      }
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

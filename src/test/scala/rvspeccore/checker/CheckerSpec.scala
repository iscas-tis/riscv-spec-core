package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class CheckerWithStateSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithState"
  it should "pass RiscvCoreTest" in {
    class TestCore extends RiscvCore {
      val checker = Module(new CheckerWithState(new RiscvCore))
      checker.io.inst  := io.inst
      checker.io.valid := io.valid
      checker.io.state := now
    }
    test(new TestCore) { c =>
      RiscvCoreTest(c)
    }
  }
}

class CheckerWithWBSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithWB"
  it should "pass RiscvCoreTest" in {
    class TestCore extends RiscvCore {
      val wb = Wire(new WriteBack)
      wb.valid := false.B
      wb.dest  := 0.U
      wb.data  := 0.U

      for (i <- 0 until 32) {
        when(now.reg(i.U) =/= next.reg(i.U)) {
          wb.valid := true.B
          wb.dest  := i.U
          wb.data  := next.reg(i.U)
        }
      }

      val checker = Module(new CheckerWithWB(new RiscvCore))
      checker.io.inst  := io.inst
      checker.io.valid := io.valid
      checker.io.pc    := now.pc
      checker.io.wb    := wb
    }
    test(new TestCore) { c =>
      RiscvCoreTest(c)
    }
  }
}

package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class CheckerWithResultSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithResult"
  it should "pass RiscvCoreTest" in {
    implicit val config = RV64Config()

    class TestCore extends RiscvCore {
      val checker = Module(new CheckerWithResult)
      checker.io.instCommit.valid := io.valid
      checker.io.instCommit.inst  := io.inst
      checker.io.instCommit.pc    := now.pc

      checker.io.result := next
    }
    test(new TestCore) { c =>
      RiscvCoreTest(c)
    }
  }
}

class CheckerWithWBSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithWB"
  it should "pass RiscvCoreTest" in {
    implicit val config = RV64Config()

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

      val checker = Module(new CheckerWithWB)
      checker.io.instCommit.valid := io.valid
      checker.io.instCommit.inst  := io.inst
      checker.io.instCommit.pc    := now.pc

      checker.io.wb := wb
    }
    test(new TestCore) { c =>
      RiscvCoreTest(c)
    }
  }
}

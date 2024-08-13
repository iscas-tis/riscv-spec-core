package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class ConnectHelperSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ConnectHelper"

  implicit val config = RVConfig(64)

  class TestCore extends RiscvCore {
    val checker = Module(new CheckerWithResult(false, false))
    checker.io.instCommit.valid := RegNext(io.valid, false.B)
    checker.io.instCommit.inst  := RegNext(io.inst)
    checker.io.instCommit.pc    := RegNext(now.pc)

    ConnectCheckerResult.setRegSource(now.reg)
    val csr = ConnectCheckerResult.makeCSRSource()
    csr := now.csr
    ConnectCheckerResult.setChecker(checker)
  }

  it should "pass RiscvTests without mem check" in {
    val testFile = RiscvTests("rv64ui", "rv64ui-addi.hex")
    test(new CoreTester(new TestCore, testFile.getCanonicalPath())) { c =>
      RiscvTests.stepTest(c, RiscvTests.maxStep)
      RiscvTests.checkReturn(c)
    }
  }
}

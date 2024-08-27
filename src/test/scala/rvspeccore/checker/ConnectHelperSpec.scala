package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class ConnectHelperSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ConnectHelper"

  class TestCore(checkMem: Boolean, checkTLB: Boolean)(implicit val config: RVConfig) extends RiscvCore {
    val checker = Module(new CheckerWithResult(checkMem, false))
    checker.io.instCommit.valid := RegNext(io.valid, false.B)
    checker.io.instCommit.inst  := RegNext(io.inst)
    checker.io.instCommit.pc    := RegNext(state.pc)

    ConnectCheckerResult.setRegSource(state.reg)
    val csr = ConnectCheckerResult.makeCSRSource()
    csr := state.csr

    if (checkMem) {
      val memSource = ConnectCheckerResult.makeMemSource()
      memSource.read.valid     := RegNext(io.mem.read.valid)
      memSource.read.addr      := RegNext(io.mem.read.addr)
      memSource.read.data      := RegNext(io.mem.read.data)
      memSource.read.memWidth  := RegNext(io.mem.read.memWidth)
      memSource.write.valid    := RegNext(io.mem.write.valid)
      memSource.write.addr     := RegNext(io.mem.write.addr)
      memSource.write.data     := RegNext(io.mem.write.data)
      memSource.write.memWidth := RegNext(io.mem.write.memWidth)
      if (checkTLB) {
        val dtlbmemSource = ConnectCheckerResult.makeTLBSource(true)
        val itlbmemSource = ConnectCheckerResult.makeTLBSource(false)
        // TODO: need implemention
      }
    }

    ConnectCheckerResult.setChecker(checker)
  }

  it should "pass RiscvTests without mem check" in {
    val testFile        = RiscvTests("rv64ui", "rv64ui-addi.hex")
    implicit val config = RVConfig(64)
    test(new CoreTester(new TestCore(false, false), testFile.getCanonicalPath())) { c =>
      RiscvTests.stepTest(c, RiscvTests.maxStep)
      RiscvTests.checkReturn(c)
    }
  }

  it should "pass RiscvTests with mem check" in {
    val testFile        = RiscvTests("rv64ui", "rv64ui-lb.hex")
    implicit val config = RVConfig(64)
    test(new CoreTester(new TestCore(true, false), testFile.getCanonicalPath())) { c =>
      RiscvTests.stepTest(c, RiscvTests.maxStep)
      RiscvTests.checkReturn(c)
    }
  }

  // TODO: need to implement a test with mem check and TLB
  // it should "pass RiscvTests with mem check and TLB" in {
  //   val testFile        = RiscvTests("rv64ui", "rv64ui-lb.hex")
  //   implicit val config = RVConfig("XLEN" -> 64, "functions" -> Seq("TLB"))
  //   test(new CoreTester(new TestCore(true, true), testFile.getCanonicalPath())) { c =>
  //     RiscvTests.stepTest(c, RiscvTests.maxStep)
  //     RiscvTests.checkReturn(c)
  //   }
  // }
}

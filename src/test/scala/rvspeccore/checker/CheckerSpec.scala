package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class CheckerWithResultSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithResult"

  implicit val config = RVConfig(64)

  class TestCore(checkMem: Boolean = true) extends RiscvCore {
    val checker = Module(new CheckerWithResult(checkMem))
    checker.io.instCommit.valid    := RegNext(io.valid, false.B)
    checker.io.instCommit.inst     := RegNext(io.inst)
    checker.io.instCommit.pc       := RegNext(now.pc)
    checker.io.event.valid         := RegNext(io.event.valid, false.B)
    checker.io.event.intrNO        := RegNext(io.event.intrNO)
    checker.io.event.cause         := RegNext(io.event.cause)
    checker.io.event.exceptionPC   := RegNext(io.event.exceptionPC)
    checker.io.event.exceptionInst := RegNext(io.event.exceptionInst)
    // printf("[  DUT   ] Valid:%x PC: %x Inst: %x\n", checker.io.instCommit.valid, checker.io.instCommit.pc, checker.io.instCommit.inst)
    checker.io.result := now

    checker.io.itlbmem.map(cm => {
      cm := DontCare
    })

    checker.io.dtlbmem.map(cm => {
      cm := DontCare
    })
    // checker.io.tlb.get.Anotherwrite := DontCare
    checker.io.mem.map(cm => {
      cm.read.addr     := mem.read.addr
      cm.read.data     := mem.read.data
      cm.read.memWidth := mem.read.memWidth
      cm.read.valid    := mem.read.valid

      cm.write.addr     := mem.write.addr
      cm.write.data     := mem.write.data
      cm.write.memWidth := mem.write.memWidth
      cm.write.valid    := mem.write.valid
    })
  }

  it should "pass RiscvTests" in {
    val tests = Seq(
      RiscvTests("rv64ui", "rv64ui-addi.hex"),
      RiscvTests("rv64ui", "rv64ui-lb.hex")
    )
    tests.foreach { testFile =>
      test(new CoreTester(new TestCore, testFile.getCanonicalPath())) { c =>
        RiscvTests.stepTest(c, RiscvTests.maxStep)
        RiscvTests.checkReturn(c)
      }
    }
  }
  // FIXME: Temporarily closed, wait for repairs.
  // it should "pass RiscvTests without mem check" in {
  //   val testFile = RiscvTests("rv64ui", "rv64ui-addi.hex")
  //   test(new CoreTester(new TestCore(false), testFile.getCanonicalPath())) { c =>
  //     RiscvTests.stepTest(c, RiscvTests.maxStep)
  //     RiscvTests.checkReturn(c)
  //   }
  // }
}

class CheckerWithWBSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithWB"

  implicit val config = RVConfig(64)

  class TestCore(checkMem: Boolean = true) extends RiscvCore {
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

    val checker = Module(new CheckerWithWB(checkMem))
    checker.io.instCommit.valid := io.valid
    checker.io.instCommit.inst  := io.inst
    checker.io.instCommit.pc    := now.pc

    checker.io.wb := wb

    checker.io.mem.map(_ := mem)
  }

  it should "pass RiscvTests" in {
    val tests = Seq(
      RiscvTests("rv64ui", "rv64ui-addi.hex"),
      RiscvTests("rv64ui", "rv64ui-lb.hex")
    )
    tests.foreach { testFile =>
      test(new CoreTester(new TestCore, testFile.getCanonicalPath())) { c =>
        RiscvTests.stepTest(c, RiscvTests.maxStep)
        RiscvTests.checkReturn(c)
      }
    }
  }
  it should "pass RiscvTests without mem check" in {
    val testFile = RiscvTests("rv64ui", "rv64ui-addi.hex")
    test(new CoreTester(new TestCore(false), testFile.getCanonicalPath())) { c =>
      RiscvTests.stepTest(c, RiscvTests.maxStep)
      RiscvTests.checkReturn(c)
    }
  }
}

package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._
import rvspeccore.core.spec.instset.csr.CSRInfoSignal

class CheckerWithResultSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithResult"

  implicit val config = RVConfig(64)

  class TestCore(checkMem: Boolean = true) extends RiscvCore {
    val checker = Module(new CheckerWithResult(checkMem = checkMem, enableReg = false))
    checker.io.instCommit.valid    := RegNext(io.valid, false.B)
    checker.io.instCommit.inst     := RegNext(io.inst)
    checker.io.instCommit.pc       := RegNext(state.pc)
    checker.io.instCommit.npc      := DontCare
    checker.io.event.valid         := RegNext(io.event.valid, false.B)
    checker.io.event.intrNO        := RegNext(io.event.intrNO)
    checker.io.event.cause         := RegNext(io.event.cause)
    checker.io.event.exceptionPC   := RegNext(io.event.exceptionPC)
    checker.io.event.exceptionInst := RegNext(io.event.exceptionInst)
    // printf("[  DUT   ] Valid:%x PC: %x Inst: %x\n", checker.io.instCommit.valid, checker.io.instCommit.pc, checker.io.instCommit.inst)
    checker.io.result := state

    checker.io.itlbmem.map(cm => {
      cm := DontCare
    })

    checker.io.dtlbmem.map(cm => {
      cm := DontCare
    })
    // checker.io.tlb.get.Anotherwrite := DontCare
    checker.io.mem.map(cm => {
      cm.read.addr     := RegNext(trans.io.mem.read.addr)
      cm.read.data     := RegNext(trans.io.mem.read.data)
      cm.read.memWidth := RegNext(trans.io.mem.read.memWidth)
      cm.read.valid    := RegNext(trans.io.mem.read.valid)

      cm.write.addr     := RegNext(trans.io.mem.write.addr)
      cm.write.data     := RegNext(trans.io.mem.write.data)
      cm.write.memWidth := RegNext(trans.io.mem.write.memWidth)
      cm.write.valid    := RegNext(trans.io.mem.write.valid)
    })
  }

  it should "pass RiscvTests with mem check" in {
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
    val tests = Seq(
      RiscvTests("rv64ui", "rv64ui-addi.hex")
    )
    tests.foreach { testFile =>
      test(new CoreTester(new TestCore(false), testFile.getCanonicalPath())) { c =>
        RiscvTests.stepTest(c, RiscvTests.maxStep)
        RiscvTests.checkReturn(c)
      }
    }
  }
}
// We have to extract some signals from RiscvCore, but it certainly modify the structure of the RiscvCore
// This can't be solved until we discuss with YiCheng about it.
class CheckerWithWBSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CheckerWithWB"

  implicit val config = RVConfig(64)

  class TestCore(checkMem: Boolean = true) extends RiscvCore {
    val wb = Wire(new WriteBack)

    wb := 0.U.asTypeOf(new WriteBack)

    for (i <- 0 until 32) {
      when(state.reg(i.U) =/= trans.io.next.reg(i.U)) {
        wb.valid := true.B
        wb.dest  := i.U
        wb.data  := trans.io.next.reg(i.U)
      }
    }

    wb.valid   := state.rd_en
    wb.dest    := state.rd_addr
    wb.data    := state.rd_data
    wb.csrAddr := state.csr_addr
    wb.r1Addr  := state.rs1_addr
    wb.r2Addr  := state.rs2_addr
    wb.r1Data  := state.reg(wb.r1Addr)
    wb.r2Data  := state.reg(wb.r2Addr)

    trans.io.next.csr.table.foreach{
      case (CSRInfoSignal(info, nextCSR)) =>
          when(wb.csrAddr === info.addr) {
            wb.csrNdata := nextCSR
          }
        }
    wb.csrWr := trans.io.next.csr_wr

    val checker = Module(new CheckerWithWB(checkMem))
    checker.io.instCommit.valid := io.valid
    checker.io.instCommit.inst  := io.inst
    checker.io.instCommit.pc    := state.pc
    checker.io.instCommit.npc   := trans.io.next.pc

    checker.io.wb := wb
    

    checker.io.result := state

    checker.io.mem.map(_ := trans.io.mem)
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

package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

class IsInstSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "IsInst"
  it should "pass test" in {
    class testDUT(instName: String, inst: UInt) extends Module {
      val out = IO(Output(Bool()))
      out := IsInst(instName, inst)
    }

    // opcode
    test(new testDUT("LUI", "b0000_0000_0000_0000_0000__00000__0110111".U(32.W))) { c => c.out.expect(true.B) }
    // opcode func3
    test(new testDUT("ADDI", "b0000_0000_0000__00000_000_00000__0010011".U(32.W))) { c => c.out.expect(true.B) }
    // opcode func3 func7
    test(new testDUT("SLLI", "b0000000__00000__00000_001_00000__0010011".U(32.W))) { c => c.out.expect(true.B) }
    // false
    test(new testDUT("ADDI", "b0000_0000_0000__00000_001_00000__0010011".U(32.W))) { c => c.out.expect(false.B) }
  }
}

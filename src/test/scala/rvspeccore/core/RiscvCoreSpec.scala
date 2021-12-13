package rvspeccore.core

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

class RiscvCoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RiscvCore"
  it should "pass ADDI test" in {
    test(new RiscvCore) { c =>
      c.io.inst.poke("b0000_0000_0001__00001__000__00010__0010011".U(32.W)) // ADDI R2 = R1 + 1
      c.io.valid.poke(true.B)

      c.io.now.reg(1).expect(0.U)
      c.io.now.reg(2).expect(0.U)
      c.io.next.reg(1).expect(0.U)
      c.io.next.reg(2).expect(1.U)
      c.clock.step()
      c.io.now.reg(1).expect(0.U)
      c.io.now.reg(2).expect(1.U)
      c.io.next.reg(1).expect(0.U)
      c.io.next.reg(2).expect(1.U)
    }
  }
}

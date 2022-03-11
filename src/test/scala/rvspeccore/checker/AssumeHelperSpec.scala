package rvspeccore.checker

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

class AssumeHelperSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "AssumeHelper"
  it should "pass test" in {
    class testDUT(instCheck: (UInt) => Bool, inst: UInt) extends Module {
      val out = IO(Output(Bool()))
      out := instCheck(inst)
    }

    test(new testDUT(RVI.LUI(_)(64), "b0000_0000_0000_0000_0000__00000__0110111".U(32.W))) { c =>
      c.out.expect(true.B)
    }
    test(new testDUT(RVG.ADDI(_)(32), "b0000_0000_0000__00000_000_00000__0010011".U(32.W))) { c =>
      c.out.expect(true.B)
    }
    test(new testDUT(RV.SLLI(_)(64), "b0000000__00000__00000_001_00000__0010011".U(32.W))) { c =>
      c.out.expect(true.B)
    }
    test(new testDUT(RV(_)(64), "b0000000__00000__00000_001_00000__0010011".U(32.W))) { c =>
      c.out.expect(true.B)
    }
    // C.AND
    test(new testDUT(RV(_)(64), "b0000_0000_0000_0000__100_0_11_001_11_010_01".U(32.W))) { c =>
      c.out.expect(true.B)
    }
    // false
    test(new testDUT(RV.ADDI(_)(64), "b0000_0000_0000__00000_001_00000__0010011".U(32.W))) { c =>
      c.out.expect(false.B)
    }
  }
}

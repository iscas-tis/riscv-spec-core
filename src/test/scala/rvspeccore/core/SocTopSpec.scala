// See README.md for license details.

package rvspeccore.core

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import rvspeccore.device.AXI4RAM
import rvspeccore.bus.axi4._
class SoC() extends Module {
    // val Spec = Module(new SpecCore)
    implicit val config = RV32Config("MC")

    val mem = Module(new AXI4RAM(new AXI4, 1024 * 1024 * 4, false, "./testcase/riscv-tests-hex/rv32ui/rv32ui-add.hex"))
    val memdelay = Module(new AXI4Delayer(0))
    val soc = Module(new SoCTop(new RiscvCore))
    memdelay.io.in <> soc.io.mem
    mem.io.in <> memdelay.io.out
}

class SocTopSpec extends AnyFreeSpec with ChiselScalatestTester {
  "SocTopSpec pass" in {
    test(new SoC()) { dut =>
      dut.clock.setTimeout(2000)
      dut.clock.step(1990)
    }
  }
}

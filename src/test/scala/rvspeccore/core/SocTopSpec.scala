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

    // because we not have axi crossbar now
    val imem = Module(new AXI4RAM(new AXI4, 1024 * 1024 * 4, false, "./testcase/riscv-tests-hex/rv32ui/rv32ui-lw.hex"))
    val dmem = Module(new AXI4RAM(new AXI4, 1024 * 1024 * 4, false, "./testcase/riscv-tests-hex/rv32ui/rv32ui-lw.hex"))
    val i_memdelay = Module(new AXI4Delayer(0))
    val d_memdelay = Module(new AXI4Delayer(0))
    val soc = Module(new SoCTop(new RiscvCore))
    i_memdelay.io.in <> soc.io.imem
    imem.io.in <> i_memdelay.io.out

    d_memdelay.io.in <> soc.io.dmem
    dmem.io.in <> d_memdelay.io.out
}

class SocTopSpec extends AnyFreeSpec with ChiselScalatestTester {
  "SocTopSpec pass" in {
    test(new SoC()) { dut =>
      dut.clock.setTimeout(300)
      dut.clock.step(200)
    }
  }
}
class GenerateSocVerilog extends AnyFreeSpec with ChiselScalatestTester {
  "GenerateSocVerilog pass" in {
    implicit val config = RV32Config("MC")
    (new chisel3.stage.ChiselStage)
      .emitVerilog(new SoCTop(new RiscvCore), Array("--target-dir", "test_run_dir/GenerateSocVerilog"))
  }
}

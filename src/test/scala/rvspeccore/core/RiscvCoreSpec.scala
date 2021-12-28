package rvspeccore.core

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

object RiscvCoreTest {
  def addi(c: RiscvCore): Unit = {
    c.reset.poke(true.B)
    c.clock.step()

    c.reset.poke(false.B)
    c.io.valid.poke(true.B)
    c.io.inst.poke("b0000_0000_0001__00001__000__00010__0010011".U(32.W)) // ADDI R2 = R1 + 1
    c.io.now.reg(1).expect(0.U)
    c.io.now.reg(2).expect(0.U)
    c.io.next.reg(1).expect(0.U)
    c.io.next.reg(2).expect(1.U)
    c.clock.step()

    c.io.now.reg(1).expect(0.U)
    c.io.now.reg(2).expect(1.U)
    c.io.next.reg(1).expect(0.U)
    c.io.next.reg(2).expect(1.U)
    c.clock.step()
  }

  val tests = Seq(
    addi(_),
    addi(_)
  )

  def apply(c: RiscvCore): Unit = {
    tests.foreach(_(c))
  }
}
object TestConfigs {
  val configs: Seq[RVConfig] = Seq(
    RV32Config(),
    RV64Config()
  )
  def apply() = configs
}

class RiscvCoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  "RiscvCore" should "pass RV64Config firrtl emit" in {
    (new chisel3.stage.ChiselStage)
      .emitFirrtl(new RiscvCore()(RV64Config()), Array("--target-dir", "test_run_dir/" + getTestName))
  }
  TestConfigs().foreach(config =>
    s"RiscvCore with ${config.getClass.getSimpleName}" should "pass RiscvCoreTest" in {
      test(new RiscvCore()(config)) { c =>
        RiscvCoreTest(c)
      }
    }
  )
}

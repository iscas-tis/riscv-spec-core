package rvspeccore.core

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import chisel3.util.experimental.loadMemoryFromFile
import java.io.File

class CoreTester(genCore: => RiscvCore, memFile: String)(implicit config: RVConfig) extends Module {
  implicit val XLEN = config.XLEN

  val bytes      = XLEN / 8
  val bytesWidth = log2Ceil(bytes)

  val io = IO(new Bundle {
    val inst = Output(UInt(32.W))
    val now  = Output(State())
  })

  val mem  = Mem(3000, UInt(XLEN.W))
  val core = Module(genCore)

  loadMemoryFromFile(mem, memFile)

  // map pc "h8000_0000".U to "h0000_0000".U
  val pc   = core.io.now.pc - "h8000_0000".U
  val inst = Wire(UInt(32.W))

  // inst
  core.io.valid := !reset.asBool
  config match {
    case RV32Config(_) => {
      val instMem = Cat(mem.read((pc >> 2) + 1.U), mem.read(pc >> 2))

      inst := MuxLookup(
        pc(1),
        0.U,
        Array(
          "b0".U(1.W) -> instMem(31, 0),
          "b1".U(1.W) -> instMem(47, 16)
        )
      )
    }
    case RV64Config(_) => {
      val instMem = Cat(mem.read((pc >> 3) + 1.U), mem.read(pc >> 3))
      inst := MuxLookup(
        pc(2, 1),
        0.U,
        Array(
          "b00".U(2.W) -> instMem(31, 0),
          "b01".U(2.W) -> instMem(47, 16),
          "b10".U(2.W) -> instMem(63, 32),
          "b11".U(2.W) -> instMem(79, 48)
        )
      )
    }
  }
  core.io.inst := inst

  def width2Mask(width: UInt): UInt = {
    MuxLookup(
      width,
      0.U(64.W),
      Array(
        8.U  -> "hff".U(64.W),
        16.U -> "hffff".U(64.W),
        32.U -> "hffff_ffff".U(64.W),
        64.U -> "hffff_ffff_ffff_ffff".U(64.W)
      )
    )
  }

  // read mem
  val rIdx  = core.io.mem.read.addr >> bytesWidth           // addr / (XLEN/8)
  val rOff  = core.io.mem.read.addr(bytesWidth - 1, 0) << 3 // addr(byteWidth-1,0) * 8
  val rMask = width2Mask(core.io.mem.read.memWidth)
  when(core.io.mem.read.valid) {
    core.io.mem.read.data := mem.read(rIdx)
  } otherwise {
    core.io.mem.read.data := 0.U
  }

  // write mem
  val wIdx  = core.io.mem.write.addr >> bytesWidth           // addr / bytes
  val wOff  = core.io.mem.write.addr(bytesWidth - 1, 0) << 3 // addr(byteWidth-1,0) * 8
  val wMask = (width2Mask(core.io.mem.write.memWidth) << wOff)(XLEN - 1, 0)
  val mData = mem.read(wIdx)
  // simulate write mask
  val wData = ((core.io.mem.write.data << wOff)(XLEN - 1, 0) & wMask) | (mData & ~wMask)
  when(core.io.mem.write.valid) {
    mem.write(wIdx, wData)
  }

  io.inst := inst
  io.now  := core.io.now
}

object RiscvTests {
  val root = "testcase/riscv-tests-hex"
  def apply(instSet: String) = {
    val set = new File(root + "/" + instSet)
    set.listFiles().filter(_.getName().endsWith(".hex")).sorted
  }
  def apply(instSet: String, instTest: String) = {
    require(instTest.endsWith(".hex"))
    new File(s"$root/$instSet/$instTest")
  }

  val maxStep = 600
  def stepTest(dut: CoreTester, restClock: Int): Int = {
    // run a clock
    dut.clock.step(1)
    if (dut.io.inst.peek().litValue == "h0000006f".U.litValue) { // end
      restClock
    } else if (restClock <= 0) { // some thing wrong
      restClock
    } else { // next step
      stepTest(dut, restClock - 1)
    }
  }
  def checkReturn(dut: CoreTester): Unit = {
    // FIXME:最后一条指令是0x6f 10号寄存器为0
    dut.io.inst.expect("h0000006f".U(32.W)) // j halt
    dut.io.now.reg(10).expect(0.U)          // li	a0,0
  }
}

class RiscvCoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "RiscvCore"
  it should "pass RV64Config firrtl emit" in {
    // generate Firrtl Code
    (new chisel3.stage.ChiselStage)
      .emitFirrtl(new RiscvCore()(RV64Config()), Array("--target-dir", "test_run_dir/" + getTestName))
  }
  it should "pass manual test" in {
    test(new RiscvCore()(RV64Config("MC"))).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.valid.poke(true.B)
      c.io.inst.poke("h8391_4441".U)
      c.clock.step()
      c.io.inst.poke("h0000_8391".U)
      c.clock.step()
      c.io.inst.poke("h0000_0000".U)
      c.clock.step()
    }
    implicit val config = RV64Config("MC")
    test(new CoreTester(new RiscvCore, "./testcase/riscv-tests-hex/rv64uc/rv64uc-rvc.hex"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        RiscvTests.stepTest(c, RiscvTests.maxStep)
        RiscvTests.checkReturn(c)
      }
  }
}

class RiscvCore64Spec extends AnyFlatSpec with ChiselScalatestTester {
  implicit val config = RV64Config("MC")

  val tests = Seq("rv64ui", "rv64um", "rv64uc")

  // NOTE: funce.i shows passed test, but RiscvCore not support it.
  //       Because RiscvCore is too simple.
  behavior of s"RiscvCore with ${config.getClass().getSimpleName()}"

  tests.foreach { testCase =>
    RiscvTests(testCase).foreach(f =>
      it should s"pass ${f.getName}" in {
        test(new CoreTester(new RiscvCore, f.getCanonicalPath())) { c =>
          RiscvTests.stepTest(c, RiscvTests.maxStep)
          RiscvTests.checkReturn(c)
        }
      }
    )
  }
}

class RiscvCore32Spec extends AnyFlatSpec with ChiselScalatestTester {
  implicit val config = RV32Config("MC")

  // val tests = Seq("rv32ui", "rv32um", "rv32uc")
  val tests = Seq("tempcsr32")

  // NOTE: funce.i shows passed test, but RiscvCore not support it.
  //       Because RiscvCore is too simple.
  behavior of s"RiscvCore with ${config.getClass().getSimpleName()}"

  tests.foreach { testCase =>
    RiscvTests(testCase).foreach(f =>
      it should s"pass ${f.getName}" in {
        test(new CoreTester(new RiscvCore, f.getCanonicalPath())) { c =>
          RiscvTests.stepTest(c, RiscvTests.maxStep)
          RiscvTests.checkReturn(c)
        }
      }
    )
  }
}
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

  val mem  = Mem(10000, UInt(XLEN.W))
  val core = Module(genCore)

  loadMemoryFromFile(mem, memFile)

  // map pc "h8000_0000".U to "h0000_0000".U
  // val pc   = core.io.now.pc - "h8000_0000".U
  // val pc2  = core.io.iFetchpc - "h8000_0000".U
  val pc = core.io.iFetchpc - "h8000_0000".U
  // printf("[Debug]From CoreSpec: PC : %x NowPC: %x iFetchpc: %x\n", pc , core.io.now.pc, core.io.iFetchpc)
  // printf("[Debug]From CoreSpec: PC2: %x NowPC: %x iFetchpc: %x\n", pc2, core.io.now.pc, core.io.iFetchpc)
  val inst      = Wire(UInt(32.W))
  val fetchAddr = Cat(mem.read((pc >> 2) + 1.U), mem.read(pc >> 2))
  // val fetchAddr2 = Cat(mem.read((pc2 >> 2) + 1.U), mem.read(pc2 >> 2))
  // printf("[Debug] InstMEM: %x %x\n", fetchAddr, fetchAddr2)
  // inst
  core.io.valid := !reset.asBool
  config.XLEN match {
    case 32 => {
      val instMem = fetchAddr

      inst := MuxLookup(pc(1), 0.U)(
        Seq(
          "b0".U(1.W) -> instMem(31, 0),
          "b1".U(1.W) -> instMem(47, 16)
        )
      )
    }
    case 64 => {
      val instMem = Cat(mem.read((pc >> 3) + 1.U), mem.read(pc >> 3))
      inst := MuxLookup(pc(2, 1), 0.U)(
        Seq(
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
    MuxLookup(width, 0.U(64.W))(
      Seq(
        8.U  -> "hff".U(64.W),
        16.U -> "hffff".U(64.W),
        32.U -> "hffff_ffff".U(64.W),
        64.U -> "hffff_ffff_ffff_ffff".U(64.W)
      )
    )
  }
  def readDatacalc(addr: UInt, memWidth: UInt): (UInt, UInt, UInt) = {
    val rIdx  = addr >> bytesWidth           // addr / (XLEN/8)
    val rOff  = addr(bytesWidth - 1, 0) << 3 // addr(byteWidth-1,0) * 8
    val rMask = width2Mask(memWidth)
    (rIdx, rOff, rMask)
  }

  // read mem
  val (rIdx, rOff, rMask) = readDatacalc(core.io.mem.read.addr, core.io.mem.read.memWidth)
  when(core.io.mem.read.valid) {
    core.io.mem.read.data := mem.read(rIdx)
  } otherwise {
    core.io.mem.read.data := 0.U
  }
  if (config.functions.tlb) {
    for (i <- 0 until 6) {
      val (rIdx, rOff, rMask) =
        readDatacalc(core.io.tlb.get.Anotherread(i).addr, core.io.tlb.get.Anotherread(i).memWidth)
      when(core.io.tlb.get.Anotherread(i).valid) {
        core.io.tlb.get.Anotherread(i).data := (mem.read(rIdx) >> rOff) & rMask
      } otherwise {
        core.io.tlb.get.Anotherread(i).data := 0.U
      }
    }
  }

  def WriteDataCalc(addr: UInt, memWidth: UInt, data: UInt): (UInt, UInt) = {
    val wIdx  = addr >> bytesWidth           // addr / bytes
    val wOff  = addr(bytesWidth - 1, 0) << 3 // addr(byteWidth-1,0) * 8
    val wMask = (width2Mask(memWidth) << wOff)(XLEN - 1, 0)
    val mData = mem.read(wIdx)
    // simulate write mask
    val wData = ((data << wOff)(XLEN - 1, 0) & wMask) | (mData & ~wMask)
    (wIdx, wData)
  }
  // write mem
  val (wIdx, wData) = WriteDataCalc(core.io.mem.write.addr, core.io.mem.write.memWidth, core.io.mem.write.data)
  when(core.io.mem.write.valid) {
    mem.write(wIdx, wData)
  }
  if (config.functions.tlb) {
    val (wIdx0, wData0) = WriteDataCalc(
      core.io.tlb.get.Anotherwrite(0).addr,
      core.io.tlb.get.Anotherwrite(0).memWidth,
      core.io.tlb.get.Anotherwrite(0).data
    )
    when(core.io.tlb.get.Anotherwrite(0).valid) {
      mem.write(wIdx0, wData0)
    }
  }
  // Begin multi port write

  // End

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

  val maxStep = 800
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
  it should "emit CHIRRTL with 64-bits RVConfig" in {
    val dirPath = "test_run_dir/" + getTestName
    new java.io.File(dirPath).mkdirs()
    val file = s"$dirPath/RiscvCore.fir"

    val writer = new java.io.PrintWriter(file)
    writer.write(
      _root_.circt.stage.ChiselStage.emitCHIRRTL(
        new RiscvCore()(RVConfig(64, "MC"))
      )
    )
    writer.close()
  }
  it should "pass manual test" in {
    test(new RiscvCore()(RVConfig(64, "MC")))
      .withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        c.io.valid.poke(true.B)
        c.io.inst.poke("h8391_4441".U)
        c.clock.step()
        c.io.inst.poke("h0000_8391".U)
        c.clock.step()
        c.io.inst.poke("h0000_0000".U)
        c.clock.step()
      }
    implicit val config = RVConfig(64, "MC")
    test(new CoreTester(new RiscvCore, "./testcase/riscv-tests-hex/rv64uc/rv64uc-rvc.hex"))
      .withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        RiscvTests.stepTest(c, RiscvTests.maxStep)
        RiscvTests.checkReturn(c)
      }
  }
}

class RiscvCore64Spec extends AnyFlatSpec with ChiselScalatestTester {
  implicit val config = RVConfig(
    XLEN = 64,
    extensions = "MCZifenceiZicsrZbaZbbZbcZbs",
    functions = Seq("Privileged")
  )

  val tests = Seq("rv64ui", "rv64um", "rv64uc", "rv64uzba", "rv64uzbb", "rv64uzbc", "rv64uzbs")

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
  implicit val config = RVConfig(
    XLEN = 32,
    extensions = "MCZifenceiZicsrZbaZbbZbcZbs",
    functions = Seq("Privileged")
  )

  val tests = Seq("rv32ui", "rv32um", "rv32uc", "rv32uzba", "rv32uzbb", "rv32uzbc", "rv32uzbs")
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

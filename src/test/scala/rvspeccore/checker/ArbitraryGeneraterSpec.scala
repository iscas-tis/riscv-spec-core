package rvspeccore.checker

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

class TestArbitraryRegFileModule(hasBug: Boolean) extends Module {
  implicit val XLEN: Int = 64
  val io = IO(new Bundle {
    val rf = Output(Vec(32, UInt(64.W)))
  })
  io.rf := ArbitraryRegFile.genSink
  ArbitraryRegFile.init

  if (hasBug)
    assert(io.rf(1) === 0.U)
  else
    assert(io.rf(0) === 0.U)
}

class ArbitraryRegFileSpec extends AnyFlatSpec with Formal with ChiselScalatestTester {
  behavior of "ArbitraryRegFile"
  it should "be able to create arbitrary regFile init value" in {
    verify(new TestArbitraryRegFileModule(false), Seq(BoundedCheck(2), BtormcEngineAnnotation))
    assertThrows[chiseltest.formal.FailedBoundedCheckException] {
      // fail because the rf(1) is Arbitrary, could be not 0.U
      // this will print a "Assertion failed at ArbitraryGeneraterSpec.scala:17 assert(io.rf(1) === 0.U)"
      verify(new TestArbitraryRegFileModule(true), Seq(BoundedCheck(2), BtormcEngineAnnotation))
    }
  }
}

package rvspeccore.checker

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec

import rvspeccore.core._

class CheckerWithStateSpec extends AnyFlatSpec with Formal with ChiselScalatestTester {
  behavior of "CheckerWithState"
  it should "pass" in {
    class TestCore extends RiscvCore {
      val checker = Module(new CheckerWithState(new RiscvCore))
      checker.io.inst  := io.inst
      checker.io.valid := io.valid
      checker.io.state := now
    }
    verify(new TestCore, Seq(BoundedCheck(3)))
  }
}

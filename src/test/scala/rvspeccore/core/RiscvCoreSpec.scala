package rvspeccore.core

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RiscvCoreSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "RiscvCore"
  it should "pass" in {
    test(new RiscvCore) { c => }
  }
}

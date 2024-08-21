package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core.spec


object GenerateArbitraryRegFile{
    def apply(implicit XLEN: Int): Vec[UInt] = {
        val rf = Wire(Vec(32, UInt(XLEN.W)))
        rf.map(_:= DontCare)
        rf(0) := 0.U
        rf
    }
}


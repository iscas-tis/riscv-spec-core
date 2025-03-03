package rvspeccore.core.tool

import chisel3._
import rvspeccore.core.BaseCore

trait CheckTool extends BaseCore {
  def updateNextWrite(rd_addr: UInt): Unit = {
    specWb.rd_en   := true.B
    specWb.rd_addr := rd_addr
    specWb.rd_data := next.reg(rd_addr)
  }

  def checkSrcImm(rs1_addr: UInt): Unit = {
    // now.reg(now.rs1_addr) := now.rs1_data
    specWb.rs1_addr := rs1_addr
    specWb.checkrs1 := true.B
  }

  def checkSrcReg(rs1_addr: UInt, rs2_addr: UInt): Unit = {
    // now.reg(rs1_addr) := now.rs1_data
    // now.reg(rs2_addr) := now.rs2_data
    specWb.rs1_addr := rs1_addr
    specWb.checkrs1 := true.B
    specWb.rs2_addr := rs2_addr
    specWb.checkrs2 := true.B
  }

  def updateNextCsrWrite(csr_addr: UInt): Unit = {
    specWb.csr_addr := csr_addr
    specWb.csr_wr   := true.B
  }
}

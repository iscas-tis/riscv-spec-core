package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import csr._

/** “Zicsr” Control and Status Register (CSR) Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 24: RV32/64G Instruction Set Listings
  *     - Table 24.2: Instruction listing for RISC-V
  */
trait ZicsrExtensionInsts {
  // - RV32/RV64 Zicsr Standard Extension
  val CSRRW  = Inst("b????????????_?????_001_?????_1110011")
  val CSRRS  = Inst("b????????????_?????_010_?????_1110011")
  val CSRRC  = Inst("b????????????_?????_011_?????_1110011")
  val CSRRWI = Inst("b????????????_?????_101_?????_1110011")
  val CSRRSI = Inst("b????????????_?????_110_?????_1110011")
  val CSRRCI = Inst("b????????????_?????_111_?????_1110011")
}

/** “Zicsr” Control and Status Register (CSR) Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 9 “Zicsr”, Control and Status Register (CSR) Instructions,
  *     Version 2.0
  */
trait ZicsrExtension extends BaseCore with CommonDecode with ZicsrExtensionInsts with CSRSupport {
  def doRVZicsr: Unit = {
    when(CSRRW(inst)) {
      decodeI
      when(rd =/= 0.U) {
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      }
      csrWrite(imm, now.reg(rs1))
    }
    when(CSRRS(inst)) {
      decodeI
      next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      when(rs1 =/= 0.U) {
        csrWrite(imm, Fill(XLEN, 1.U(1.W)), now.reg(rs1))
      }
    }
    when(CSRRC(inst)) {
      decodeI
      next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      when(rs1 =/= 0.U) {
        csrWrite(imm, 0.U(XLEN.W), now.reg(rs1))
      }
    }
    when(CSRRWI(inst)) {
      decodeI
      when(rd =/= 0.U) {
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      }
      csrWrite(imm, zeroExt(rs1, XLEN))
    }
    when(CSRRSI(inst)) {
      decodeI
      next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      when(rs1 =/= 0.U) {
        csrWrite(imm, Fill(XLEN, 1.U(1.W)), zeroExt(rs1, XLEN))
      }
    }
    when(CSRRCI(inst)) {
      decodeI
      next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
      when(rs1 =/= 0.U) {
        csrWrite(imm, 0.U(XLEN.W), zeroExt(rs1, XLEN))
      }
    }
  }
}

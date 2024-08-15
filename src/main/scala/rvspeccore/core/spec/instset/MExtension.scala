package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

/** “M” Standard Extension Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 24: RV32/64G Instruction Set Listings
  *     - Table 24.2: Instruction listing for RISC-V
  */
trait MExtensionInsts {
  // - RV32M Standard Extension
  val MUL    = Inst("b0000001_?????_?????_000_?????_0110011")
  val MULH   = Inst("b0000001_?????_?????_001_?????_0110011")
  val MULHSU = Inst("b0000001_?????_?????_010_?????_0110011")
  val MULHU  = Inst("b0000001_?????_?????_011_?????_0110011")
  val DIV    = Inst("b0000001_?????_?????_100_?????_0110011")
  val DIVU   = Inst("b0000001_?????_?????_101_?????_0110011")
  val REM    = Inst("b0000001_?????_?????_110_?????_0110011")
  val REMU   = Inst("b0000001_?????_?????_111_?????_0110011")

  // - RV64M Standard Extension (in addition to RV32M)
  val MULW  = Inst("b0000001_?????_?????_000_?????_0111011")
  val DIVW  = Inst("b0000001_?????_?????_100_?????_0111011")
  val DIVUW = Inst("b0000001_?????_?????_101_?????_0111011")
  val REMW  = Inst("b0000001_?????_?????_110_?????_0111011")
  val REMUW = Inst("b0000001_?????_?????_111_?????_0111011")
}

// scalafmt: { maxColumn = 200 }

/** “M” Standard Extension for Integer Multiplication and Division
  *
  *   - riscv-spec-20191213
  *   - Chapter 7: “M” Standard Extension for Integer Multiplication and
  *     Division, Version 2.0
  */
trait MExtension extends BaseCore with CommonDecode with MExtensionInsts {
  // - Table 7.1: Semantics for division by zero and division overflow.
  // : L is the width of the operation in bits: XLEN for DIV[U] and REM[U],
  // : or 32 for DIV[U]W and REM[U]W.
  def opDIV(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      (divisor.asSInt / dividend.asSInt)(L - 1, 0).asUInt, // (L-1, 0) cut extra bit in double sign bit
      Seq(
        (dividend === 0.U(L.W))                                                      -> -1.S(L.W).asUInt,
        (divisor === -(1 << (L - 1)).S(L.W).asUInt && dividend === -1.S(L.W).asUInt) -> -(1 << (L - 1)).S(L.W).asUInt
      )
    )
  }
  def opDIVU(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      divisor / dividend,
      Seq(
        (dividend === 0.U(L.W)) -> Fill(L, 1.U(1.W))
      )
    )
  }
  def opREM(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      (divisor.asSInt % dividend.asSInt).asUInt,
      Seq(
        (dividend === 0.U(L.W))                                                      -> divisor,
        (divisor === -(1 << (L - 1)).S(L.W).asUInt && dividend === -1.S(L.W).asUInt) -> 0.U(L.W)
      )
    )
  }
  def opREMU(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      divisor % dividend,
      Seq(
        (dividend === 0.U(L.W)) -> divisor
      )
    )
  }

  def doRV32M: Unit = {
    // - 7.1 Multiplication Operations
    // - MUL/MULH[[S]U]
    when(MUL(inst))    { decodeR; next.reg(rd) := (now.reg(rs1) * now.reg(rs2))(XLEN - 1, 0) }
    when(MULH(inst))   { decodeR; next.reg(rd) := (now.reg(rs1).asSInt * now.reg(rs2).asSInt).asUInt(XLEN * 2 - 1, XLEN) }
    when(MULHSU(inst)) { decodeR; next.reg(rd) := (now.reg(rs1).asSInt * now.reg(rs2)).asUInt(XLEN * 2 - 1, XLEN) }
    when(MULHU(inst))  { decodeR; next.reg(rd) := (now.reg(rs1) * now.reg(rs2))(XLEN * 2 - 1, XLEN) }
    // - 7.2 Division Operations
    // - DIV[U]/REM[U]
    when(DIV(inst))  { decodeR; next.reg(rd) := opDIV(now.reg(rs1), now.reg(rs2), XLEN) }
    when(DIVU(inst)) { decodeR; next.reg(rd) := opDIVU(now.reg(rs1), now.reg(rs2), XLEN) }
    when(REM(inst))  { decodeR; next.reg(rd) := opREM(now.reg(rs1), now.reg(rs2), XLEN) }
    when(REMU(inst)) { decodeR; next.reg(rd) := opREMU(now.reg(rs1), now.reg(rs2), XLEN) }
  }

  def doRV64M: Unit = {
    doRV32M

    // - 7.1 Multiplication Operations
    // - MULW
    when(MULW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0) * now.reg(rs2)(31, 0))(31, 0), XLEN) }
    // - 7.2 Division Operations
    // - DIV[U]W/REM[U]W
    when(DIVW(inst))  { decodeR; next.reg(rd) := signExt(opDIV(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
    when(DIVUW(inst)) { decodeR; next.reg(rd) := signExt(opDIVU(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
    when(REMW(inst))  { decodeR; next.reg(rd) := signExt(opREM(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
    when(REMUW(inst)) { decodeR; next.reg(rd) := signExt(opREMU(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
  }

  def doRVM: Unit = {
    XLEN match {
      case 32 => doRV32M
      case 64 => doRV64M
    }
  }
}

// scalafmt: { maxColumn = 120 } (back to defaults)

package rvspeccore.core.spec.behavior

import chisel3._
import chisel3.util._

import rvspeccore.core._
import rvspeccore.core.spec.code._
import rvspeccore.core.tool.BitTool._

// scalafmt: { maxColumn = 200 }

/** “M” Standard Extension for Integer Multiplication and Division
  *
  *   - riscv-spec-20191213
  *   - Chapter 7: “M” Standard Extension for Integer Multiplication and
  *     Division, Version 2.0
  */
trait MExtension extends BaseCore { this: Decode =>
  // Table 7.1: Semantics for division by zero and division overflow.
  // L is the width of the operation in bits:
  // XLEN for DIV[U] and REM[U], or 32 for DIV[U]W and REM[U]W.
  def opDIV(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      (divisor.asSInt / dividend.asSInt)(L - 1, 0).asUInt, // (L-1, 0) cut extra bit in double sign bit
      Array(
        (dividend === 0.U(L))                                                        -> -1.S(L.W).asUInt,
        (divisor === -(1 << (L - 1)).S(L.W).asUInt && dividend === -1.S(L.W).asUInt) -> -(1 << (L - 1)).S(L.W).asUInt
      )
    )
  }
  def opDIVU(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      divisor / dividend,
      Array(
        (dividend === 0.U(L.W)) -> Fill(L, 1.U(1.W))
      )
    )
  }
  def opREM(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      (divisor.asSInt % dividend.asSInt).asUInt,
      Array(
        (dividend === 0.U(L))                                                        -> divisor,
        (divisor === -(1 << (L - 1)).S(L.W).asUInt && dividend === -1.S(L.W).asUInt) -> 0.U(L)
      )
    )
  }
  def opREMU(divisor: UInt, dividend: UInt, L: Int): UInt = {
    MuxCase(
      divisor % dividend,
      Array(
        (dividend === 0.U(L)) -> divisor
      )
    )
  }

  def deRV32M: Unit = {
    switch(inst(6, 0)) {
      is(OpcodeMap("OP")) {
        rTypeDecode
        switch(Cat(funct7, funct3)) {
          // 7.1 Multiplication Operations
          // MUL/MULH[[S]U]
          is(catLit(Funct7Map("MULDIV"), Funct3Map("MUL")))    { next.reg(rd) := (now.reg(rs1) * now.reg(rs2))(XLEN - 1, 0) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("MULH")))   { next.reg(rd) := (now.reg(rs1).asSInt * now.reg(rs2).asSInt).asUInt(XLEN * 2 - 1, XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("MULHU")))  { next.reg(rd) := (now.reg(rs1) * now.reg(rs2))(XLEN * 2 - 1, XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("MULHSU"))) { next.reg(rd) := (now.reg(rs1).asSInt * now.reg(rs2)).asUInt(XLEN * 2 - 1, XLEN) }
          // 7.2 Division Operations
          // DIV[U]/REM[U]
          is(catLit(Funct7Map("MULDIV"), Funct3Map("DIV")))  { next.reg(rd) := opDIV(now.reg(rs1), now.reg(rs2), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("DIVU"))) { next.reg(rd) := opDIVU(now.reg(rs1), now.reg(rs2), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("REM")))  { next.reg(rd) := opREM(now.reg(rs1), now.reg(rs2), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("REMU"))) { next.reg(rd) := opREMU(now.reg(rs1), now.reg(rs2), XLEN) }
        }
      }
    }
  }
  def deRV64M: Unit = {
    deRV32M
    switch(inst(6, 0)) {
      is(OpcodeMap("OP-32")) {
        rTypeDecode
        switch(Cat(funct7, funct3)) {
          // 7.1 Multiplication Operations
          // MULW
          is(catLit(Funct7Map("MULDIV"), Funct3Map("MULW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) * now.reg(rs2)(31, 0))(31, 0), XLEN) }
          // 7.2 Division Operations
          // DIV[U]W/REM[U]W
          is(catLit(Funct7Map("MULDIV"), Funct3Map("DIVW")))  { next.reg(rd) := signExt(opDIV(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("DIVUW"))) { next.reg(rd) := signExt(opDIVU(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("REMW")))  { next.reg(rd) := signExt(opREM(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
          is(catLit(Funct7Map("MULDIV"), Funct3Map("REMUW"))) { next.reg(rd) := signExt(opREMU(now.reg(rs1)(31, 0), now.reg(rs2)(31, 0), 32), XLEN) }
        }
      }
    }
  }
}

// scalafmt: { maxColumn = 120 } (back to defaults)

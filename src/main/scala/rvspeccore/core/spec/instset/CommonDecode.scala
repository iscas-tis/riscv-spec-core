package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.tool.BitTool._

/** Decode part
  *
  *   - riscv-spec-20191213
  *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
  *   - 2.3 Immediate Encoding Variants
  */
trait CommonDecode extends BaseCore with csr.ExceptionSupport {
  val opcode = WireInit(0.U(7.W))
  val rd     = WireInit(0.U(5.W))
  val funct3 = WireInit(0.U(3.W))
  val rs1    = WireInit(0.U(5.W))
  val rs2    = WireInit(0.U(5.W))
  val funct7 = WireInit(0.U(7.W))
  val imm    = WireInit(0.U(XLEN.W))

  // Figure 2.3: RISC-V base instruction formats showing immediate variants
  //   31                   25 | 24        20 | 19 15 | 14    12 | 11                   7 | 6      0
  // /-------------------------|--------------|-------|----------|------------------------|----------\
  // | funct7                7 | rs2        5 | rs1 5 | funct3 3 | rd                   5 | opcode 7 | R-type
  // | imm_11_0                            12 | rs1 5 | funct3 3 | rd                   5 | opcode 7 | I-type
  // | imm_11_5              7 | rs2        5 | rs1 5 | funct3 3 | imm_4_0              5 | opcode 7 | S-type
  // | imm_12  1 | imm_10_5  6 | rs2        5 | rs1 5 | funct3 3 | imm_4_1  4 | imm_11  1 | opcode 7 | B-type
  // | imm_31_12                                              20 | rd                   5 | opcode 7 | U-type
  // | imm_20  1 | imm_10_1    10 | imm_11  1 | imm_19_12      8 | rd                   5 | opcode 7 | J-type
  // \-----------|----------------|-----------|------------------|------------|-----------|----------/
  //   31     31 | 30          21 | 20     20 | 19            12 | 11       8 | 7       7 | 6      0

  // temp imm
  // scalafmt: { maxColumn = 200 }
  val imm_11_0  = WireInit(0.U(12.W))
  val imm_11_5  = WireInit(0.U(7.W)); val imm_4_0  = WireInit(0.U(5.W))
  val imm_12    = WireInit(0.U(1.W)); val imm_10_5 = WireInit(0.U(6.W)); val imm_4_1    = WireInit(0.U(4.W)); val imm_11 = WireInit(0.U(1.W))
  val imm_31_12 = WireInit(0.U(20.W))
  val imm_20    = WireInit(0.U(1.W)); val imm_10_1 = WireInit(0.U(10.W)); val imm_19_12 = WireInit(0.U(8.W))
  // scalafmt: { maxColumn = 120 } (back to defaults)

  // format: off
  //                                     / 31           25 | 24    20 | 19 15 | 14 12 | 11           7 | 6    0 \
  def decodeR = { decodeInit; unpack(List( funct7          , rs2      , rs1   , funct3, rd             , opcode ), inst)                                                                           }
  def decodeI = { decodeInit; unpack(List( imm_11_0                   , rs1   , funct3, rd             , opcode ), inst); imm := signExt(    imm_11_0                                      , XLEN) }
  def decodeS = { decodeInit; unpack(List( imm_11_5        , rs2      , rs1   , funct3, imm_4_0        , opcode ), inst); imm := signExt(Cat(imm_11_5, imm_4_0)                            , XLEN) }
  def decodeB = { decodeInit; unpack(List( imm_12, imm_10_5, rs2      , rs1   , funct3, imm_4_1, imm_11, opcode ), inst); imm := signExt(Cat(imm_12, imm_11, imm_10_5, imm_4_1, 0.U(1.W))  , XLEN) }
  def decodeU = { decodeInit; unpack(List( imm_31_12                                  , rd             , opcode ), inst); imm := signExt(Cat(imm_31_12, 0.U(12.W))                         , XLEN) }
  def decodeJ = { decodeInit; unpack(List( imm_20, imm_10_1   , imm_11, imm_19_12     , rd             , opcode ), inst); imm := signExt(Cat(imm_20, imm_19_12, imm_11, imm_10_1, 0.U(1.W)), XLEN) }
  //                                     \ 31 31 | 30      21 | 20 20 | 19         12 | 11   8 | 7   7 | 6    0 /
  // format: on

  def decodeInit = {
    // decode only work when the instruction is legal
    legalInstruction()
  }
}

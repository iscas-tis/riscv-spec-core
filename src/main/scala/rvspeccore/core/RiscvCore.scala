package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import tool.BitTool._

abstract class BaseCore()(implicit config: RVConfig) extends Module {
  implicit val width: Int = config.width
}

/** Decode part
  *
  *   - riscv-spec-20191213
  *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
  *   - 2.3 Immediate Encoding Variants
  */
trait Decode extends BaseCore {
  val inst = WireInit(0.U(32.W))

  val opcode = WireInit(0.U(7.W))
  val rd     = WireInit(0.U(5.W))
  val funct3 = WireInit(0.U(3.W))
  val rs1    = WireInit(0.U(5.W))
  val rs2    = WireInit(0.U(5.W))
  val funct7 = WireInit(0.U(7.W))
  val imm    = WireInit(0.U(width.W))

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
  //                             / 31           25 | 24    20 | 19 15 | 14 12 | 11           7 | 6    0 \
  def rTypeDecode = { unpack(List( funct7          , rs2      , rs1   , funct3, rd             , opcode ), inst)                                                                            }
  def iTypeDecode = { unpack(List( imm_11_0                   , rs1   , funct3, rd             , opcode ), inst); imm := signExt(    imm_11_0                                      , width) }
  def sTypeDecode = { unpack(List( imm_11_5        , rs2      , rs1   , funct3, imm_4_0        , opcode ), inst); imm := signExt(Cat(imm_11_5, imm_4_0)                            , width) }
  def bTypeDecode = { unpack(List( imm_12, imm_10_5, rs2      , rs1   , funct3, imm_4_1, imm_11, opcode ), inst); imm := signExt(Cat(imm_12, imm_11, imm_10_5, imm_4_1, 0.U(1.W))  , width) }
  def uTypeDecode = { unpack(List( imm_31_12                                  , rd             , opcode ), inst); imm := signExt(Cat(imm_31_12, 0.U(12.W))                         , width) }
  def jTypeDecode = { unpack(List( imm_20, imm_10_1   , imm_11, imm_19_12     , rd             , opcode ), inst); imm := signExt(Cat(imm_20, imm_19_12, imm_11, imm_10_1, 0.U(1.W)), width) }
  //                             \ 31 31 | 30      21 | 20 20 | 19         12 | 11   8 | 7   7 | 6    0 /
  // format: on
}

class State()(implicit width: Int) extends Bundle {
  val reg = Vec(32, UInt(width.W))
  val pc  = UInt(width.W)

  def init(reg: UInt = 0.U(width.W), pc: UInt = "h8000_0000".U(width.W)): State = {
    val state = Wire(this)
    state.reg := Seq.fill(32)(reg)
    state.pc  := pc
    state
  }
}

object State {
  def apply()(implicit width: Int): State = new State
}

class RiscvCore()(implicit config: RVConfig) extends BaseCore with Decode {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val now  = Output(State())
    val next = Output(State())
  })

  val now  = RegInit(State().init())
  val next = Wire(State())

  // should keep the value in the next clock
  // if there no changes below
  next := now

  // ID & EXE
  when(io.valid) {
    inst := io.inst
    // scalafmt: { maxColumn = 200 }
    switch(inst(6, 0)) {
      // riscv-spec-20191213
      // Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
      // 2.4 Integer Computational Instructions
      // Integer Register-Immediate Instructions
      is(OpcodeMap("OP-IMM")) {
        iTypeDecode
        switch(funct3) {
          // ADDI/SLTI[U]
          is(Funct3Map("ADDI")) { next.reg(rd) := now.reg(rs1) + imm }
          is(Funct3Map("SLTI")) { next.reg(rd) := Mux(now.reg(rs1).asSInt < imm.asSInt, 1.U, 0.U) }
          is(Funct3Map("SLTU")) { next.reg(rd) := Mux(now.reg(rs1) < imm, 1.U, 0.U) }
          // ANDI/ORI/XORI
          is(Funct3Map("ANDI")) { next.reg(rd) := now.reg(rs1) & imm }
          is(Funct3Map("ORI"))  { next.reg(rd) := now.reg(rs1) | imm }
          is(Funct3Map("XORI")) { next.reg(rd) := now.reg(rs1) ^ imm }
        }
        switch(Cat(imm(11, 5), funct3)) {
          // SLLI/SRLI/SRAI
          is(catLit("b000_0000".U(7.W), Funct3Map("SLLI"))) { next.reg(rd) := now.reg(rs1) << imm(4, 0) }
          is(catLit("b000_0000".U(7.W), Funct3Map("SRLI"))) { next.reg(rd) := now.reg(rs1) >> imm(4, 0) }
          is(catLit("b010_0000".U(7.W), Funct3Map("SRAI"))) { next.reg(rd) := (now.reg(rs1).asSInt >> imm(4, 0)).asUInt }
        }
      }
      is(OpcodeMap("LUI")) {
        uTypeDecode
        // LUI
        next.reg(rd) := imm
      }
      is(OpcodeMap("AUIPC")) {
        uTypeDecode
        // AUIPC
        next.reg(rd) := now.pc + imm
      }
    }
    // scalafmt: { maxColumn = 120 } (back to defaults)

    next.pc := now.pc + 4.U
  }

  // update
  now := next

  // output
  io.now  := now
  io.next := next
}

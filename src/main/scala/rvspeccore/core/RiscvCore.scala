package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import tool.BitTool._

/** Decode path
  *
  *   - riscv-spec-20191213
  *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
  *   - 2.3 Immediate Encoding Variants
  */
trait Decode extends Module {
  val inst = WireInit(0.U(32.W))

  val opcode = WireInit(0.U(7.W))
  val rd     = WireInit(0.U(5.W))
  val funct3 = WireInit(0.U(3.W))
  val rs1    = WireInit(0.U(5.W))
  val rs2    = WireInit(0.U(5.W))
  val funct7 = WireInit(0.U(7.W))
  val imm    = WireInit(0.U(64.W))

  // Figure 2.3: RISC-V base instruction formats showing immediate variants
  //   31                   25 | 24        20 | 19 15 | 14    12 | 11                   7 | 6      0
  // /-------------------------|--------------|-------|----------|------------------------|----------\
  // | funct7                7 | rs2        5 | rs1 5 | funct3 3 | rd                   5 | opcode 7 | R-type
  // | imm[11:0]                           12 | rs1 5 | funct3 3 | rd                   5 | opcode 7 | I-type
  // | imm[11:5]             7 | rs2        5 | rs1 5 | funct3 3 | imm[4:0]             5 | opcode 7 | S-type
  // | imm[12] 1 | imm[10:5] 6 | rs2        5 | rs1 5 | funct3 3 | imm[4:1] 4 | imm[11] 1 | opcode 7 | B-type
  // | imm[31:12]                                             20 | rd                   5 | opcode 7 | U-type
  // | imm[20] 1 | imm[10:1]   10 | imm[11] 1 | imm[19:12]     8 | rd                   5 | opcode 7 | J-type
  // \-----------|----------------|-----------|------------------|------------|-----------|----------/
  //   31     31 | 30          21 | 20     20 | 19            12 | 11       8 | 7       7 | 6      0

  def ITypeDecode = {
    opcode := inst(6, 0)
    rd     := inst(11, 7)
    funct3 := inst(14, 12)
    rs1    := inst(19, 15)
    imm    := signExt(inst(31, 20), 64)
  }
}

class State extends Bundle {
  val reg = Vec(32, UInt(64.W))
  val pc  = UInt(64.W)
}

object State {
  def apply(reg: UInt = 0.U(64.W), pc: UInt = "h8000_0000".U(64.W)): State = {
    val state = Wire(new State)
    state.reg := Seq.fill(32)(reg)
    state.pc  := pc
    state
  }
}

class RiscvCore extends Module with Decode {
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())

    val now  = Output(new State)
    val next = Output(new State)
  })

  val now  = RegInit(State())
  val next = Wire(new State)

  // should keep the value in the next clock
  // if there no changes below
  next := now

  // ID & EXE
  when(io.valid) {
    inst := io.inst
    switch(inst(6, 0)) {
      // riscv-spec-20191213
      // Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
      // 2.4 Integer Computational Instructions
      // Integer Register-Immediate Instructions
      is(OpcodeMap("OP-IMM")) {
        ITypeDecode
        switch(funct3) {
          is(Funct3Map("ADDI")) { next.reg(rd) := now.reg(rs1) + imm }
          is(Funct3Map("ANDI")) { next.reg(rd) := now.reg(rs1) & imm }
        }
      }
    }

    next.pc := now.pc + 4.U
  }

  // update
  now := next

  // output
  io.now  := now
  io.next := next
}

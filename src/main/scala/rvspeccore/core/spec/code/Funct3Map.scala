package rvspeccore.core.spec.code

import chisel3._
import chisel3.util._

object Funct3Map {

  /** funct3 code map
    *
    *   - riscv-spec-20191213
    *   - Chapter 24: RV32/64G Instruction Set Listings
    *   - Table 24.2: Instruction listing for RISC-V
    */
  val funct3Map: Map[String, UInt] = Map(
    // RV32I Base Instruction Set
    // Jump
    "JALR" -> "b000",
    // Logic
    "BEQ"  -> "b000",
    "BNE"  -> "b001",
    "BLT"  -> "b100",
    "BGE"  -> "b101",
    "BLTU" -> "b110",
    "BGEU" -> "b111",
    // Load Store
    "LB"  -> "b000",
    "LH"  -> "b001",
    "LW"  -> "b010",
    "LBU" -> "b100",
    "LHU" -> "b101",
    "SB"  -> "b000",
    "SH"  -> "b001",
    "SW"  -> "b010",
    // Arith with imm
    "ADDI"  -> "b000",
    "SLTI"  -> "b010",
    "SLTIU" -> "b011",
    "XORI"  -> "b100",
    "ORI"   -> "b110",
    "ANDI"  -> "b111",
    "SLLI"  -> "b001",
    "SRLI"  -> "b101",
    "SRAI"  -> "b101",
    // Arith without imm
    "ADD"  -> "b000",
    "SUB"  -> "b000",
    "SLL"  -> "b001",
    "SLT"  -> "b010",
    "SLTU" -> "b011",
    "XOR"  -> "b100",
    "SRL"  -> "b101",
    "SRA"  -> "b101",
    "OR"   -> "b110",
    "AND"  -> "b111",
    // System
    "FENCE"  -> "b000",
    "ECALL"  -> "b000",
    "EBREAK" -> "b000",

    // RV64I Base Instruction Set
    "LWU"   -> "b110",
    "LD"    -> "b011",
    "SD"    -> "b011",
    "SLLI"  -> "b001",
    "SRLI"  -> "b101",
    "SRAI"  -> "b101",
    "ADDIW" -> "b000",
    "SLLIW" -> "b001",
    "SRLIW" -> "b101",
    "SRAIW" -> "b101",
    "ADDW"  -> "b000",
    "SUBW"  -> "b000",
    "SLLW"  -> "b001",
    "SRLW"  -> "b101",
    "SRAW"  -> "b101",

    // RV32M Standard Extension
    "MUL"    -> "b000",
    "MULH"   -> "b001",
    "MULHSU" -> "b010",
    "MULHU"  -> "b011",
    "DIV"    -> "b100",
    "DIVU"   -> "b101",
    "REM"    -> "b110",
    "REMU"   -> "b111",

    // RV32M Standard Extension
    "MULW"  -> "b000",
    "DIVW"  -> "b100",
    "DIVUW" -> "b101",
    "REMW"  -> "b110",
    "REMUW" -> "b111"
  ).map(x => (x._1 -> x._2.U(3.W)))

  def contains(instName: String): Boolean = {
    funct3Map.contains(instName)
  }

  def apply(instName: String): UInt = {
    funct3Map(instName)
  }
}

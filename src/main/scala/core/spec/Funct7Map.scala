package core.spec

import chisel3._
import chisel3.util._

object Funct7Map {

  /** funct7 code map
    *
    *   - riscv-spec-20191213
    *   - Chapter 24: RV32/64G Instruction Set Listings
    *   - Table 24.2: Instruction listing for RISC-V
    *   - RV32I Base Instruction Set, RV64I Base Instruction Set (in addition to
    *     RV32I)
    */
  val funct7Map: Map[String, UInt] = Map(
    // RV32I Base Instruction Set
    // Arith with imm
    "SLLI" -> "b0000000",
    "SRLI" -> "b0000000",
    "SRAI" -> "b0100000",
    // Arith without imm
    "ADD"  -> "b0000000",
    "SUB"  -> "b0100000",
    "SLL"  -> "b0000000",
    "SLT"  -> "b0000000",
    "SLTU" -> "b0000000",
    "XOR"  -> "b0000000",
    "SRL"  -> "b0000000",
    "SRA"  -> "b0100000",
    "OR"   -> "b0000000",
    "AND"  -> "b0000000",

    // RV64I Base Instruction Set
    "SLLIW" -> "b0000000",
    "SRLIW" -> "b0000000",
    "SRAIW" -> "b0100000",
    "ADDW"  -> "b0000000",
    "SUBW"  -> "b0100000",
    "SLLW"  -> "b0000000",
    "SRLW"  -> "b0000000",
    "SRAW"  -> "b0100000"
  ).map(x => (x._1 -> x._2.U(7.W)))

  def apply(instName: String): UInt = {
    funct7Map(instName)
  }
}

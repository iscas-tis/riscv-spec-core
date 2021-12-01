package core.spec

import chisel3._
import chisel3.util._

object Funct3Map {

  /** funct3 code map
    *
    *   - riscv-spec-20191213
    *   - Chapter 24: RV32/64G Instruction Set Listings
    *   - Table 24.2: Instruction listing for RISC-V
    *   - RV32I Base Instruction Set, RV64I Base Instruction Set (in addition to
    *     RV32I)
    */
  val funct3Map = Map(
    "JALR" -> "b000".U(3.W),
    "BEQ"  -> "b000".U(3.W),
    "ADDI" -> "b000".U(3.W),
    "ANDI" -> "b111".U(3.W)
    // TODO: to complete
  )

  def apply(instName: String): UInt = {
    funct3Map(instName)
  }
}
